package fr.ftnl.cardgame.twitch

import fr.ftnl.cardgame.domain.game.ChatCardAccess
import fr.ftnl.cardgame.domain.game.ChatCardSettings
import java.text.Normalizer

/** Which pile a viewer is writing into. */
enum class ChatCardKind { SITUATION, PUNCHLINE }

/** A card a viewer typed, once the line has been read but before anybody let it through. */
data class ChatProposal(
    val kind: ChatCardKind,
    val text: String,
    val viewerId: String,
    val viewerName: String,
)

/**
 * Turns a chat line into the card it is offering, if it is offering one.
 *
 * Only an explicit command counts — `!situation ...`, `!réponse ...` — because a chat that
 * writes onto the table by accident is a chat nobody dares open. Beyond that the syntax
 * gives way: the case is free, the accents are optional, and `!situ` and `!rep` say the
 * same thing as the long forms. A viewer typing fast at two in the morning should not have
 * their idea dropped over a circumflex.
 *
 * A cheer arrives with its cheermotes glued into the text (`Cheer100 le pire c'est ____`),
 * and those are Twitch's word rather than the viewer's, so they come back out.
 */
object ChatCardCommand {

    /**
     * The words that open a card, short forms included. Nobody in a chat types
     * `!réponse` twice — by the third round it is `!rep`, so `!rep` is a first class way
     * of saying it rather than an alias somebody has to discover.
     *
     * They are matched as whole words against a lower-cased, unaccented token, so
     * `!RÉPONSE`, `!Reponse` and `!rep` are the same command. Anything else is somebody
     * talking, and stays out of the deck.
     */
    private val SITUATIONS = setOf("situ", "situs", "situation", "situations")
    private val PUNCHLINES = setOf(
        "rep", "reps", "repo", "reponse", "reponses",
        "punch", "punchline", "punchlines",
        "carte", "cartes",
    )

    /** `Cheer100`, `PogChamp250` — a cheermote is a word ending in the amount it is worth. */
    private val CHEERMOTE = Regex("""\b[A-Za-z]+\d+\b""")

    /** Control characters, which have no business being printed on a card. */
    private val NOISE = Regex("[\\u0000-\\u001F\\u007F]")

    private val WHITESPACE = Regex("""\s+""")

    /** What `Normalizer` leaves behind once an accent has been split off its letter. */
    private val COMBINING = Regex("\\p{Mn}+")

    const val MIN_LENGTH = 3
    const val MAX_LENGTH = 200

    fun parse(line: ChatLine): ChatProposal? {
        val raw = line.text.trim()
        if (!raw.startsWith('!')) return null
        // `! situation …` is a slip, not a different command, so the bang is peeled off
        // before the word is read rather than glued to it.
        val spoken = raw.drop(1).trimStart()
        val kind = kindOf(spoken.substringBefore(' ')) ?: return null
        val body = clean(
            spoken.substringAfter(' ', missingDelimiterValue = ""),
            stripCheermotes = line.bits > 0,
        )
        if (body.length !in MIN_LENGTH..MAX_LENGTH) return null
        return ChatProposal(kind, body, line.viewerId, line.viewerName)
    }

    private fun kindOf(word: String): ChatCardKind? = when (plain(word)) {
        in SITUATIONS -> ChatCardKind.SITUATION
        in PUNCHLINES -> ChatCardKind.PUNCHLINE
        else -> null
    }

    /** Lower case, and without the accents a viewer should never have to get right. */
    private fun plain(word: String): String =
        COMBINING.replace(Normalizer.normalize(word, Normalizer.Form.NFD), "").lowercase()

    private fun clean(text: String, stripCheermotes: Boolean): String {
        val withoutNoise = NOISE.replace(text, " ")
        val withoutCheers = if (stripCheermotes) CHEERMOTE.replace(withoutNoise, " ") else withoutNoise
        return WHITESPACE.replace(withoutCheers, " ").trim()
    }

    /**
     * Whether this line paid what the host is asking. The price itself is set on Twitch —
     * the streamer prices their own reward, and their own cheer — so all that is checked
     * here is that Twitch says it was paid, and that it was the right reward when the host
     * pinned one down.
     */
    fun allows(rules: ChatCardSettings, line: ChatLine): Boolean = when (rules.access) {
        ChatCardAccess.OFF -> false
        ChatCardAccess.EVERYONE -> true
        ChatCardAccess.BITS -> line.bits > 0 && line.bits >= rules.minBits
        ChatCardAccess.CHANNEL_POINTS ->
            line.rewardId != null && (rules.rewardId.isBlank() || rules.rewardId == line.rewardId)
    }

    /** Whether the pile this proposal is aimed at is open at all. */
    fun accepts(rules: ChatCardSettings, kind: ChatCardKind): Boolean = when (kind) {
        ChatCardKind.SITUATION -> rules.situations
        ChatCardKind.PUNCHLINE -> rules.punchlines
    }
}
