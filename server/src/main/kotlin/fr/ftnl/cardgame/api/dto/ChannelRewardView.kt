package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/**
 * One channel point reward already standing on the host's channel, as the lobby offers it.
 *
 * [managed] is the whole point of the list. Twitch lets an application fulfil or cancel a
 * redemption only on a reward that application created, so a reward built in the streamer's
 * dashboard can be read but never answered for — the lobby has to say which is which before
 * the host picks one.
 */
@Serializable
data class ChannelRewardView(
    val id: String,
    val title: String,
    val cost: Int,
    /** True when the game created it, and may therefore settle and refund its redemptions. */
    val managed: Boolean,
)
