package fr.ftnl.cardgame.domain.card

import kotlinx.serialization.Serializable

/**
 * The sentence printed on a situation card, where a run of underscores marks a hole
 * that a punchline fills. A text without any hole expects a single answer appended at the end.
 */
@Serializable
@JvmInline
value class SituationText(val raw: String) {

    init {
        require(raw.isNotBlank()) { "A situation text cannot be blank" }
    }

    /** How many punchlines a player must play for this situation, always at least one. */
    val blankCount: Int
        get() = PLACEHOLDER.findAll(raw).count().coerceAtLeast(1)

    /** Renders the situation with [answers] injected, leaving unfilled holes untouched. */
    fun fill(answers: List<String>): String =
        if (PLACEHOLDER.containsMatchIn(raw)) fillPlaceholders(answers) else appendAnswers(answers)

    private fun fillPlaceholders(answers: List<String>): String {
        var index = 0
        return PLACEHOLDER.replace(raw) { match ->
            answers.getOrNull(index++)?.let(::inline) ?: match.value
        }
    }

    private fun appendAnswers(answers: List<String>): String =
        (listOf(raw) + answers.map(::inline)).joinToString(" ").trim()

    private fun inline(answer: String): String = answer.trim().trimEnd('.')

    override fun toString(): String = raw

    private companion object {
        val PLACEHOLDER = Regex("_{2,}")
    }
}
