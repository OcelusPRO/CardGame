package fr.ftnl.cardgame.api

import fr.ftnl.cardgame.api.dto.ChannelRewardView
import fr.ftnl.cardgame.api.dto.ErrorResponse
import fr.ftnl.cardgame.auth.TwitchHostTokens
import fr.ftnl.cardgame.auth.playerSession
import fr.ftnl.cardgame.twitch.TwitchRewardCards
import fr.ftnl.cardgame.twitch.TwitchRewards
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

/**
 * The channel point rewards of the signed in host's own channel.
 *
 * There is no consent flow here: the right to manage these rewards is granted once, on the
 * Twitch sign in itself, so a host who is signed in is already able to use them. What is
 * left is reading what stands on their channel, and handing the right back.
 */
fun Route.twitchRewardRoutes(
    tokens: TwitchHostTokens,
    rewardCards: TwitchRewardCards,
    rewards: TwitchRewards,
) {
    /**
     * What is already standing on the host's channel, so the lobby can offer it rather
     * than only ever making new ones.
     *
     * Each entry says whether the game may manage it, because that is what the host is
     * really choosing between: a reward the game created can be settled and refunded, and
     * a reward the streamer built in their dashboard cannot — its redemptions will sit in
     * their own queue waiting to be validated by hand.
     */
    get("/api/twitch/rewards") {
        val twitchId = call.playerSession().twitchId
            ?: return@get call.respond(HttpStatusCode.Unauthorized, ErrorResponse("NO_TWITCH"))
        val token = tokens.token(twitchId)
            ?: return@get call.respond(
                HttpStatusCode.Conflict,
                ErrorResponse(
                    "NOT_AUTHORIZED",
                    "Reconnectez-vous avec Twitch pour donner au jeu accès à vos points de chaîne.",
                ),
            )
        val all = rewards.list(token, twitchId, onlyManageable = false)
            ?: return@get call.respond(
                HttpStatusCode.BadGateway,
                ErrorResponse(
                    "REWARDS_UNAVAILABLE",
                    "Twitch n'a pas donné la liste des récompenses. Les points de chaîne demandent une chaîne affiliée ou partenaire.",
                ),
            )
        val ours = rewards.list(token, twitchId, onlyManageable = true)?.map { it.id }?.toSet().orEmpty()
        call.respond(all.map { ChannelRewardView(it.id, it.title, it.cost, managed = it.id in ours) })
    }

    /**
     * Handing the right back without signing out. The rewards still standing on that
     * channel are taken down first, while there is still a token to take them down with —
     * a right withdrawn must not leave a reward behind pointing at a game nobody can play.
     */
    post("/api/twitch/rewards/forget") {
        call.playerSession().twitchId?.let { twitchId ->
            rewardCards.release(twitchId)
            tokens.forget(twitchId)
        }
        call.respond(HttpStatusCode.NoContent)
    }
}
