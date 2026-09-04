package fr.ftnl.cardgame.twitch

import fr.ftnl.cardgame.domain.game.SubmissionId

/**
 * Turns a chat line into the answer it points at. Viewers are shown one-based numbers,
 * the way the cards are labelled on screen, and the domain counts from zero.
 *
 * Only a line that is *nothing but* a vote counts: "2", "!2", "#2", "vote 2", "!vote #2".
 * A number caught in a sentence ("la 2 est nulle") is somebody talking, not voting, and
 * counting it would let the loudest chatter vote several times by accident.
 */
object ChatVote {

    private val PATTERN = Regex("""^!?\s*(?:vote\s*)?#?(\d{1,2})$""", RegexOption.IGNORE_CASE)

    fun parse(line: String, answerCount: Int): SubmissionId? {
        val number = PATTERN.find(line.trim())?.groupValues?.get(1)?.toIntOrNull() ?: return null
        if (number !in 1..answerCount) return null
        return SubmissionId(number - 1)
    }
}
