package fr.ftnl.cardgame.domain.game

import kotlinx.serialization.Serializable

/**
 * Point values of a round, in one currency.
 *
 * [pointsPerVote] is what a single approving voice is worth: every vote received in
 * [SelectionMode.VOTE], and the pick of the card czar in [SelectionMode.CZAR]. On top of
 * that, an answer chosen by everyone who could choose it earns [unanimityBonus]; a single
 * voter picking something else cancels it.
 */
@Serializable
data class ScoringSettings(
    val pointsPerVote: Int = 1,
    val unanimityBonus: Int = 3,
) {
    init {
        require(pointsPerVote in 1..MAX_POINTS) { "pointsPerVote must be within 1..$MAX_POINTS" }
        require(unanimityBonus in 0..MAX_POINTS) { "unanimityBonus must be within 0..$MAX_POINTS" }
    }

    companion object {
        const val MAX_POINTS = 20
    }
}
