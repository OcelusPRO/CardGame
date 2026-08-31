package fr.ftnl.cardgame.domain.support

import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.card.CardPool
import fr.ftnl.cardgame.domain.card.PunchlineCard
import fr.ftnl.cardgame.domain.card.SituationCard
import fr.ftnl.cardgame.domain.card.SituationText
import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.domain.game.GameSettings
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.game.Scoreboard
import fr.ftnl.cardgame.domain.player.Avatar
import fr.ftnl.cardgame.domain.player.AvatarPart
import fr.ftnl.cardgame.domain.player.Nickname
import fr.ftnl.cardgame.domain.player.Player
import fr.ftnl.cardgame.domain.player.PlayerId

/** Ready made pieces of game so each test only spells out what it actually asserts. */
object GameFixtures {

    val CODE: GameCode = GameCode.of("ABCDE")

    fun avatar(): Avatar = Avatar(AvatarPart("head-1", "#ff8800"), AvatarPart("body-1", "#3355ff"))

    fun player(name: String): Player = Player(PlayerId(name), Nickname.of(name), avatar())

    fun players(vararg names: String): List<Player> = names.map(::player)

    fun situation(id: String, text: String = "Le pire cadeau de Noël, c'est ____."): SituationCard =
        SituationCard(CardId(id), SituationText(text))

    fun punchline(id: String): PunchlineCard = PunchlineCard(CardId(id), "punchline $id")

    fun pool(situations: Int = 4, punchlines: Int = 80): CardPool = CardPool(
        situations = (1..situations).map { situation("s$it") },
        punchlines = (1..punchlines).map { punchline("p$it") },
    )

    fun lobby(players: List<Player>, settings: GameSettings = duoFriendly()): GameState = GameState(
        code = CODE,
        hostId = players.first().id,
        players = players,
        settings = settings,
        scoreboard = players.fold(Scoreboard()) { board, player -> board.withPlayer(player.id) },
        createdAtMillis = 1_000_000,
    )

    /** Default settings with a low player floor, so a test can run a table of two. */
    fun duoFriendly(): GameSettings = GameSettings(minPlayers = 2)
}
