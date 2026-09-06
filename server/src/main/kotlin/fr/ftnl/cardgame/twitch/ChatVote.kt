package fr.ftnl.cardgame.twitch

import fr.ftnl.cardgame.domain.game.SubmissionId

/**
 * Turns a chat line into the answer it points at. Viewers are shown one-based numbers, the
 * way the cards are labelled on screen, and the domain counts from zero.
 *
 * The numbers are **positions on screen**, not answer ids: a chat judging a whole round
 * picks among all of them, and a chat judging a duel picks between exactly two — so `2`
 * means the second card in front of them either way. Which is why the choices are handed
 * in rather than derived from a count.
 *
 * Only a line that is *nothing but* a vote counts: "2", "!2", "#2", "vote 2", "!vote #2".
 * A number caught in a sentence ("la 2 est nulle") is somebody talking, not voting, and
 * counting it would let the loudest chatter vote several times by accident.
 */
object ChatVote {

    private val PATTERN = Regex("""^!?\s*(?:vote\s*)?#?(\d{1,2})$""", RegexOption.IGNORE_CASE)

    fun parse(line: String, choices: List<SubmissionId>): SubmissionId? {
        val number = PATTERN.find(line.trim())?.groupValues?.get(1)?.toIntOrNull() ?: return null
        return choices.getOrNull(number - 1)
    }
}
