package fr.ftnl.cardgame.twitch

import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.card.SituationCard
import fr.ftnl.cardgame.domain.card.SituationText
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameSettings
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.game.Round
import fr.ftnl.cardgame.domain.game.SelectionMode
import fr.ftnl.cardgame.domain.game.Submission
import fr.ftnl.cardgame.domain.game.SubmissionId
import fr.ftnl.cardgame.domain.player.Avatar
import fr.ftnl.cardgame.domain.player.AvatarPart
import fr.ftnl.cardgame.domain.player.Nickname
import fr.ftnl.cardgame.domain.player.Player
import fr.ftnl.cardgame.domain.player.PlayerId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The bridge between a Twitch chat and a round. The reader is faked: what is under test
 * is the counting, the one-voice-per-viewer rule, the faces kept for the table, and when
 * the listener bothers to watch at all.
 */
class TwitchChatVotingTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val code = GameCode.of("ABCDE")
    private val alice = PlayerId("alice")
    private val bob = PlayerId("bob")

    @AfterTest
    fun tearDown() = scope.cancel()

    @Test
    fun `one viewer, one voice, and the tally reaches the game`() = runBlocking {
        val pushed = Channel<GameCommand.SetChatVotes>(Channel.UNLIMITED)
        val voting = voting(
            reader(
                line("1", id = "1"),
                line("2", id = "2"),
                // The same viewer again: their first vote is the one that stands.
                line("2", id = "1"),
                line("mdr", id = "3"),
            ),
            pushed,
        )

        voting.onGameChanged(selecting(), emptyList())

        val tallies = withTimeout(TIMEOUT) { pushed.receive() }.tallies
        assertEquals(mapOf(SubmissionId(0) to 1, SubmissionId(1) to 1), tallies.mapValues { it.value.count })
    }

    @Test
    fun `a viewer only votes once, whichever chat they type it in`() = runBlocking {
        val pushed = Channel<GameCommand.SetChatVotes>(Channel.UNLIMITED)
        val voting = voting(
            reader(line("1", id = "7", channel = "kameto"), line("2", id = "7", channel = "ponce")),
            pushed,
        )

        voting.onGameChanged(selecting(guests = true), emptyList())

        val tallies = withTimeout(TIMEOUT) { pushed.receive() }.tallies
        assertEquals(mapOf(SubmissionId(0) to 1), tallies.mapValues { it.value.count })
    }

    @Test
    fun `the first faces are carried, the crowd behind them is a number`() = runBlocking {
        val pushed = Channel<GameCommand.SetChatVotes>(Channel.UNLIMITED)
        val crowd = (1..40).map { line("1", id = "$it", name = "Viewer $it") }
        val voting = voting(reader(*crowd.toTypedArray()), pushed)

        voting.onGameChanged(selecting(), emptyList())

        val tally = withTimeout(TIMEOUT) { pushed.receive() }.tallies.getValue(SubmissionId(0))
        assertEquals(40, tally.count)
        assertEquals(15, tally.voters.size)
        assertEquals("Viewer 1", tally.voters.first().name)
    }

    @Test
    fun `the faces shown carry the pictures their chat shows`() = runBlocking {
        val pushed = Channel<GameCommand.SetChatVotes>(Channel.UNLIMITED)
        val pictures = ViewerPictures { ids -> ids.associateWith { "https://pictures.example/$it.png" } }
        val voting = voting(reader(line("1", id = "9")), pushed, pictures)

        voting.onGameChanged(selecting(), emptyList())

        val tally = withTimeout(TIMEOUT) { pushed.receive() }.tallies.getValue(SubmissionId(0))
        assertEquals("https://pictures.example/9.png", tally.voters.single().avatarUrl)
    }

    @Test
    fun `nothing is read while nobody is judging`() = runBlocking {
        val pushed = Channel<GameCommand.SetChatVotes>(Channel.UNLIMITED)
        val voting = voting(reader(line("1")), pushed)

        voting.onGameChanged(selecting().copy(phase = GamePhase.SUBMITTING), emptyList())
        delay(200)

        assertTrue(pushed.tryReceive().isFailure, "a chat was read outside the vote")
    }

    @Test
    fun `nothing is read on a table no streamer sits at`() = runBlocking {
        val pushed = Channel<GameCommand.SetChatVotes>(Channel.UNLIMITED)
        val voting = voting(reader(line("1")), pushed)

        val plain = selecting().let { it.copy(players = it.players.map { p -> p.copy(twitchLogin = null) }) }
        voting.onGameChanged(plain, emptyList())
        delay(200)

        assertTrue(pushed.tryReceive().isFailure, "a chat was read for nobody")
    }

    private fun voting(
        reader: TwitchChatReader,
        pushed: Channel<GameCommand.SetChatVotes>,
        pictures: ViewerPictures = ViewerPictures.NONE,
    ) = TwitchChatVoting(reader, scope, pictures, flushMillis = 20) { _, command ->
        if (command is GameCommand.SetChatVotes) pushed.send(command)
    }

    private fun line(text: String, id: String = "1", name: String = "Viewer", channel: String = "kameto") =
        ChatLine(channel = channel, viewerId = id, viewerName = name, text = text)

    /** Replays a handful of lines, then keeps the connection open like a real chat would. */
    private fun reader(vararg lines: ChatLine) = TwitchChatReader { _, onLine ->
        lines.forEach { onLine(it) }
        awaitCancellation()
    }

    private fun selecting(guests: Boolean = false): GameState {
        val settings =
            GameSettings(selectionMode = SelectionMode.CHAT, twitchGuestChats = guests, minPlayers = 2)
        return GameState(
            code = code,
            hostId = alice,
            players = listOf(player(alice, "kameto"), player(bob, "ponce")),
            settings = settings,
            phase = GamePhase.SELECTING,
            round = Round(
                number = 1,
                situation = SituationCard(CardId("s1"), SituationText("Le pire, c'est ____.")),
                submissions = mapOf(
                    alice to Submission(alice, texts = listOf("un chat mouillé")),
                    bob to Submission(bob, texts = listOf("la honte")),
                ),
                revealOrder = listOf(alice, bob),
            ),
        )
    }

    private fun player(id: PlayerId, twitchLogin: String) = Player(
        id = id,
        nickname = Nickname.of(id.value),
        avatar = Avatar(AvatarPart("head-1", "#ff8800"), AvatarPart("body-1", "#3355ff")),
        twitchLogin = twitchLogin,
    )

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
