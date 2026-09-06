package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** The round as the viewer is allowed to see it. */
@Serializable
data class RoundView(
    val number: Int,
    val situation: SituationCardView,
    val expectedAnswers: Int,
    val czarId: String? = null,
    val answers: List<AnswerView> = emptyList(),
    val myVote: Int? = null,
    /** Where the knockout ladder stands, in the duel mode only. */
    val bracket: BracketView? = null,
    val outcome: RoundOutcomeView? = null,
)

/**
 * The ladder as the table sees it: which two answers are facing off, how far along the
 * round is, and what each answer has won so far.
 */
@Serializable
data class BracketView(
    /** One-based: tier 1 is the first pairing, the last tier is the final. */
    val tier: Int,
    /** Position of the open duel inside its tier, and how many that tier holds. */
    val duelNumber: Int,
    val duelCount: Int,
    /** The two answers on the table; [right] is absent when the duel is a walkover. */
    val left: Int? = null,
    val right: Int? = null,
    /** Duels won so far, per answer. This is what the round pays out on. */
    val wins: List<DuelWinsView> = emptyList(),
    val championId: Int? = null,
)

@Serializable
data class DuelWinsView(val answerId: Int, val wins: Int)
