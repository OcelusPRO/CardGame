package fr.ftnl.cardgame.twitch

import fr.ftnl.cardgame.api.dto.ExtensionCardRequest
import fr.ftnl.cardgame.api.dto.ExtensionProductView
import fr.ftnl.cardgame.api.dto.ExtensionRewardView
import fr.ftnl.cardgame.api.dto.ExtensionStateView
import fr.ftnl.cardgame.config.TwitchExtensionConfig
import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.card.CardOrigin
import fr.ftnl.cardgame.domain.card.PunchlineCard
import fr.ftnl.cardgame.domain.card.SituationCard
import fr.ftnl.cardgame.domain.card.SituationText
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.game.ChatCardAccess
import fr.ftnl.cardgame.domain.game.ChatCardReward
import fr.ftnl.cardgame.domain.game.ChatCardSettings
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.game.DispatchResult
import fr.ftnl.cardgame.game.GameService
import io.ktor.http.HttpStatusCode
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** What came of a card sent from the panel. */
sealed interface ExtensionOutcome {
    data class Accepted(val message: String) : ExtensionOutcome
    data class Refused(val status: HttpStatusCode, val code: String, val message: String) :
        ExtensionOutcome
}

/**
 * The panel side of "the chat writes cards".
 *
 * It answers the same rules as the chat commands do — the host sets one policy, and it
 * governs both the `!situation` typed in chat and the form under the stream. What the
 * panel adds is bits: a viewer buys the right to propose in one click, and Twitch hands
 * back a signed receipt that is checked here rather than taken at face value.
 *
 * A receipt is spent once. Twitch retries a transaction the panel failed to acknowledge,
 * so without that a viewer replaying their own receipt would get a card per replay.
 */
class ExtensionCardService(
    private val games: GameService,
    private val channels: TwitchChannelIndex,
) {
    private val spentReceipts = ConcurrentHashMap.newKeySet<String>()

    suspend fun state(channelId: String, config: TwitchExtensionConfig): ExtensionStateView {
        val game = gameOf(channelId) ?: return ExtensionStateView(open = false)
        val rules = game.settings.chatCards
        if (!rules.enabled) return ExtensionStateView(open = false)
        return ExtensionStateView(
            open = true,
            situations = rules.situations,
            punchlines = rules.punchlines,
            access = rules.access.name,
            products = if (rules.access == ChatCardAccess.BITS) products(config, rules) else emptyList(),
            rewards = if (rules.access == ChatCardAccess.CHANNEL_POINTS) rewards(rules) else emptyList(),
            written = game.chatCardCount,
            limit = ChatCardSettings.MAX_CARDS_PER_GAME,
        )
    }

    suspend fun submit(
        viewer: ExtensionViewer,
        request: ExtensionCardRequest,
        config: TwitchExtensionConfig,
        tokens: ExtensionTokens,
    ): ExtensionOutcome {
        val game = gameOf(viewer.channelId)
            ?: return refused(HttpStatusCode.NotFound, "NO_GAME", "Aucune partie en cours sur cette chaîne.")
        val rules = game.settings.chatCards
        if (!rules.enabled) {
            return refused(HttpStatusCode.Conflict, "CHAT_CARDS_CLOSED", "L'hôte n'a pas ouvert les propositions.")
        }
        val kind = kindOf(request.kind)
            ?: return refused(HttpStatusCode.BadRequest, "BAD_KIND", "Carte inconnue.")
        if (!ChatCardCommand.accepts(rules, kind)) {
            return refused(HttpStatusCode.Conflict, "CHAT_CARDS_CLOSED", "Ce type de carte est fermé.")
        }
        val text = request.text.trim()
        if (text.length !in ChatCardCommand.MIN_LENGTH..ChatCardCommand.MAX_LENGTH) {
            return refused(
                HttpStatusCode.BadRequest,
                "BAD_TEXT",
                "Entre ${ChatCardCommand.MIN_LENGTH} et ${ChatCardCommand.MAX_LENGTH} caractères.",
            )
        }
        paymentRefusal(viewer, request, rules, config, tokens)?.let { return it }
        return dispatch(game, kind, text)
    }

    /**
     * Whether this viewer paid what the host is asking.
     *
     * Bits are settled here, against a receipt Twitch signed. Channel points are not: a
     * redemption never reaches an extension — it reaches the game through EventSub — so in
     * that mode the panel simply names the reward, and the viewer redeems it the usual way.
     */
    private fun paymentRefusal(
        viewer: ExtensionViewer,
        request: ExtensionCardRequest,
        rules: ChatCardSettings,
        config: TwitchExtensionConfig,
        tokens: ExtensionTokens,
    ): ExtensionOutcome.Refused? = when (rules.access) {
        ChatCardAccess.OFF -> refusedPayment("CHAT_CARDS_CLOSED", "Les propositions sont fermées.")
        ChatCardAccess.EVERYONE -> null
        ChatCardAccess.CHANNEL_POINTS -> refusedPayment(
            "USE_CHANNEL_POINTS",
            "Sur cette chaîne, une carte se propose en échangeant la récompense de points de chaîne.",
        )
        ChatCardAccess.BITS -> bitsRefusal(viewer, request, rules, config, tokens)
    }

    private fun bitsRefusal(
        viewer: ExtensionViewer,
        request: ExtensionCardRequest,
        rules: ChatCardSettings,
        config: TwitchExtensionConfig,
        tokens: ExtensionTokens,
    ): ExtensionOutcome.Refused? {
        val receipt = tokens.receipt(request.receipt)
            ?: return refusedPayment("BITS_REQUIRED", "Cette proposition se paie en bits.")
        val paid = config.bitsFor(receipt.sku)
            ?: return refusedPayment("UNKNOWN_PRODUCT", "Ce produit n'est pas reconnu.")
        if (paid < rules.minBits) {
            return refusedPayment("NOT_ENOUGH_BITS", "L'hôte demande au moins ${rules.minBits} bits.")
        }
        // A receipt is one card. Twitch replays a transaction the panel never acknowledged,
        // and a viewer can replay their own just as easily.
        if (!spentReceipts.add(receiptKey(viewer, receipt))) {
            return refusedPayment("ALREADY_USED", "Ce paiement a déjà servi.")
        }
        forgetOldReceipts()
        return null
    }

    private suspend fun dispatch(game: GameState, kind: ChatCardKind, text: String): ExtensionOutcome {
        val command = when (kind) {
            ChatCardKind.SITUATION -> GameCommand.AddChatCards(
                situations = listOf(
                    SituationCard(CardId("chat-s-${UUID.randomUUID()}"), SituationText(text), CardOrigin.CHAT),
                ),
            )
            ChatCardKind.PUNCHLINE -> GameCommand.AddChatCards(
                punchlines = listOf(
                    PunchlineCard(CardId("chat-p-${UUID.randomUUID()}"), text, CardOrigin.CHAT),
                ),
            )
        }
        return when (val result = games.dispatch(game.code, command)) {
            is DispatchResult.Updated -> ExtensionOutcome.Accepted("Votre carte est dans le paquet.")
            is DispatchResult.Refused -> refused(
                HttpStatusCode.Conflict,
                result.error.name,
                "La table n'a pas pris la carte.",
            )
            DispatchResult.GameNotFound ->
                refused(HttpStatusCode.NotFound, "NO_GAME", "La partie vient de se terminer.")
        }
    }

    private suspend fun gameOf(channelId: String): GameState? =
        channels.gameOf(channelId)?.let { games.find(it) }

    /** The rewards actually standing on the channel, in the order the piles are offered. */
    private fun rewards(rules: ChatCardSettings) = listOfNotNull(
        rules.situationReward.takeIf { rules.situations }?.let { view(it, ChatCardReward.SITUATION) },
        rules.punchlineReward.takeIf { rules.punchlines }?.let { view(it, ChatCardReward.PUNCHLINE) },
    )

    private fun view(reward: ChatCardReward, fallback: ChatCardReward) =
        ExtensionRewardView(reward.title.ifBlank { fallback.title }, reward.cost)

    private fun products(config: TwitchExtensionConfig, rules: ChatCardSettings) = config.products
        .filterValues { it >= rules.minBits }
        .map { (sku, bits) -> ExtensionProductView(sku, bits) }
        .sortedBy { it.bits }

    private fun kindOf(raw: String): ChatCardKind? =
        ChatCardKind.entries.firstOrNull { it.name == raw.trim().uppercase() }

    private fun receiptKey(viewer: ExtensionViewer, receipt: BitsReceipt): String =
        "${viewer.channelId}:${receipt.transactionId}"

    /** A server that runs for weeks should not remember every cheer it ever saw. */
    private fun forgetOldReceipts() {
        if (spentReceipts.size > MAX_REMEMBERED_RECEIPTS) spentReceipts.clear()
    }

    private fun refused(status: HttpStatusCode, code: String, message: String) =
        ExtensionOutcome.Refused(status, code, message)

    private fun refusedPayment(code: String, message: String) =
        ExtensionOutcome.Refused(HttpStatusCode.PaymentRequired, code, message)

    private companion object {
        const val MAX_REMEMBERED_RECEIPTS = 20_000
    }
}
