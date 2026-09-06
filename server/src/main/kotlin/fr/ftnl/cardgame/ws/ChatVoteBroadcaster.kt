package fr.ftnl.cardgame.ws

import fr.ftnl.cardgame.api.dto.ChatAnswerVotesView
import fr.ftnl.cardgame.api.dto.ChatVoterView
import fr.ftnl.cardgame.domain.game.ChatVoteTally
import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.domain.game.SubmissionId
import fr.ftnl.cardgame.twitch.ChatVoteLive
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Pushes the running chat tally to the screens watching a table.
 *
 * Unlike [GameBroadcaster] it builds one payload for everybody: a chat count is public by
 * definition — it is written on the stream — so there is nothing here to hide from anyone,
 * and nothing to project per viewer.
 */
class ChatVoteBroadcaster(private val connections: GameConnections) : ChatVoteLive {

    override suspend fun push(
        code: GameCode,
        tallies: Map<SubmissionId, ChatVoteTally>,
    ): Unit = coroutineScope {
        val message = ServerMessage.ChatVotes(tallies.map { (id, tally) -> answerView(id, tally) })
        connections.of(code).forEach { connection -> launch { connection.send(message) } }
    }

    private fun answerView(id: SubmissionId, tally: ChatVoteTally) = ChatAnswerVotesView(
        id = id.index,
        count = tally.count,
        voters = tally.voters.map { ChatVoterView(it.id, it.name, it.avatarUrl) },
    )
}
