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
    val outcome: RoundOutcomeView? = null,
)
