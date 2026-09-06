package fr.ftnl.cardgame

import fr.ftnl.cardgame.api.view.GameViewFactory
import fr.ftnl.cardgame.auth.AccountDirectory
import fr.ftnl.cardgame.auth.AccountLookup
import fr.ftnl.cardgame.auth.AdminGuard
import fr.ftnl.cardgame.auth.AdultAccessGuard
import fr.ftnl.cardgame.auth.DiscordClient
import fr.ftnl.cardgame.auth.TwitchClient
import fr.ftnl.cardgame.catalog.AdminCardService
import fr.ftnl.cardgame.catalog.AdminPackService
import fr.ftnl.cardgame.catalog.AdultAccessService
import fr.ftnl.cardgame.catalog.AdultPackAccessRepository
import fr.ftnl.cardgame.catalog.CardPackRepository
import fr.ftnl.cardgame.catalog.CardPoolResolver
import fr.ftnl.cardgame.catalog.CatalogService
import fr.ftnl.cardgame.catalog.CustomCardFactory
import fr.ftnl.cardgame.catalog.ExposedAdultPackAccessRepository
import fr.ftnl.cardgame.catalog.ExposedCardPackRepository
import fr.ftnl.cardgame.catalog.ExposedPunchlineCardRepository
import fr.ftnl.cardgame.catalog.ExposedSituationCardRepository
import fr.ftnl.cardgame.catalog.PunchlineCardRepository
import fr.ftnl.cardgame.catalog.SituationCardRepository
import fr.ftnl.cardgame.config.AppConfig
import fr.ftnl.cardgame.domain.deck.RandomShuffler
import fr.ftnl.cardgame.domain.engine.GameEngine
import fr.ftnl.cardgame.domain.game.GameClock
import fr.ftnl.cardgame.domain.game.RandomGameCodeGenerator
import fr.ftnl.cardgame.domain.game.SystemGameClock
import fr.ftnl.cardgame.game.GameCodeAllocator
import fr.ftnl.cardgame.game.GameDecks
import fr.ftnl.cardgame.game.GameEntryService
import fr.ftnl.cardgame.game.GameFactory
import fr.ftnl.cardgame.game.GameLocks
import fr.ftnl.cardgame.game.GameService
import fr.ftnl.cardgame.game.IdleGameReaper
import fr.ftnl.cardgame.game.PhaseScheduler
import fr.ftnl.cardgame.plugins.ApiJson
import fr.ftnl.cardgame.plugins.prometheusRegistry
import fr.ftnl.cardgame.session.GameSessionCodec
import fr.ftnl.cardgame.session.GameSessionStore
import fr.ftnl.cardgame.session.InMemoryGameSessionStore
import fr.ftnl.cardgame.session.RedisGameSessionStore
import fr.ftnl.cardgame.stats.ExposedUsageStatsReader
import fr.ftnl.cardgame.stats.ExposedUsageStatsWriter
import fr.ftnl.cardgame.stats.StatsRecorder
import fr.ftnl.cardgame.stats.StatsService
import fr.ftnl.cardgame.stats.UsageStatsReader
import fr.ftnl.cardgame.stats.UsageStatsWriter
import fr.ftnl.cardgame.twitch.ChatVoteBoard
import fr.ftnl.cardgame.twitch.ExtensionCardService
import fr.ftnl.cardgame.twitch.ExtensionTokens
import fr.ftnl.cardgame.twitch.TwitchAppTokens
import fr.ftnl.cardgame.twitch.TwitchChannelIndex
import fr.ftnl.cardgame.twitch.TwitchChatCards
import fr.ftnl.cardgame.twitch.TwitchChatReader
import fr.ftnl.cardgame.twitch.TwitchChatSocket
import fr.ftnl.cardgame.twitch.TwitchChatVoting
import fr.ftnl.cardgame.twitch.TwitchViewers
import fr.ftnl.cardgame.twitch.ViewerPictures
import fr.ftnl.cardgame.ws.ChatVoteBroadcaster
import fr.ftnl.cardgame.ws.GameBroadcaster
import fr.ftnl.cardgame.ws.GameCommandTranslator
import fr.ftnl.cardgame.ws.GameConnections
import fr.ftnl.cardgame.ws.GameSocketHandler
import io.ktor.client.HttpClient
import io.micrometer.core.instrument.Gauge
import kotlinx.coroutines.CoroutineScope
import redis.clients.jedis.JedisPooled

/**
 * The composition root. Everything is wired here, once, explicitly: no reflection, and
 * a test can swap any collaborator by handing over its own instance.
 */
class ApplicationServices(
    val config: AppConfig,
    scope: CoroutineScope,
    val httpClient: HttpClient,
    sessionStore: GameSessionStore? = null,
    val clock: GameClock = SystemGameClock,
    chatReader: TwitchChatReader = TwitchChatSocket(httpClient, config.twitch.chatUrl),
) {
    private val redis: JedisPooled? =
        if (sessionStore == null && config.redis.enabled) JedisPooled(config.redis.url) else null

    val connections = GameConnections()

    val packRepository: CardPackRepository = ExposedCardPackRepository()
    val situationRepository: SituationCardRepository = ExposedSituationCardRepository()
    val punchlineRepository: PunchlineCardRepository = ExposedPunchlineCardRepository()
    val adultAccessRepository: AdultPackAccessRepository = ExposedAdultPackAccessRepository()
    private val statsWriter: UsageStatsWriter = ExposedUsageStatsWriter()
    private val statsReader: UsageStatsReader = ExposedUsageStatsReader()

    val discordClient = DiscordClient(httpClient)
    val twitchClient = TwitchClient(httpClient, config.twitch.clientId)
    private val twitchTokens = TwitchAppTokens(httpClient, config.twitch)

    /** Puts a name on an id, and turns a Twitch channel name into the id behind it. */
    val accounts: AccountLookup = AccountDirectory(discordClient, config.discord, twitchClient, twitchTokens)

    val adultAccessGuard = AdultAccessGuard(adultAccessRepository, config.admin, clock, config.adultAccess)
    val catalog = CatalogService(packRepository, situationRepository, punchlineRepository)
    val adminPacks = AdminPackService(packRepository, situationRepository, punchlineRepository, clock)
    val adminCards = AdminCardService(situationRepository, punchlineRepository, clock)
    val adminAdultAccess = AdultAccessService(adultAccessRepository, clock, accounts)
    private val deckResolver = CardPoolResolver(packRepository, situationRepository, punchlineRepository)
    val appliedDecks = GameDecks()

    val sessions: GameSessionStore = sessionStore ?: redis?.let {
        RedisGameSessionStore(it, GameSessionCodec(), config.redis.sessionTtlMinutes)
    } ?: InMemoryGameSessionStore()

    val games = GameService(
        store = sessions,
        engine = GameEngine(RandomShuffler(), clock),
        locks = GameLocks(),
        codes = GameCodeAllocator(RandomGameCodeGenerator(), sessions),
        factory = GameFactory(clock),
    )

    /** Where the running chat tally lives, deliberately outside the game snapshot. */
    val chatVotes = ChatVoteBoard()

    /** Which table a Twitch channel is playing at, for the extension panel. */
    val twitchChannels = TwitchChannelIndex()

    val extensionTokens = ExtensionTokens(config.twitchExtension)
    val extensionCards = ExtensionCardService(games, twitchChannels)

    val metrics = prometheusRegistry()

    val views = GameViewFactory(clock)
    val entry = GameEntryService(games, deckResolver, appliedDecks, adultAccessGuard)
    val statsService = StatsService(statsReader, packRepository, situationRepository, punchlineRepository, connections, clock)
    val adminGuard = AdminGuard(config.admin)

    val socketHandler = GameSocketHandler(
        games = games,
        connections = connections,
        views = views,
        translator = GameCommandTranslator(deckResolver, CustomCardFactory(), appliedDecks),
        adultAccess = adultAccessGuard,
        scope = scope,
        json = ApiJson,
    )

    /**
     * The chat tally is banked into the round here, and nowhere else: right before the
     * judging closes, which is the moment it stops being a display and becomes a score.
     */
    private val scheduler = PhaseScheduler(
        scope = scope,
        clock = clock,
        beforeDeadline = { state -> listOfNotNull(chatVotes.commitFor(state)) },
    ) { code, command -> games.dispatch(code, command) }

    /** A table left completely untouched for half an hour is dropped. */
    private val idleReaper = IdleGameReaper(scope, IDLE_GAME_MILLIS) { code -> games.forget(code) }

    /**
     * Idle until a host actually asks for it: with nobody signed in with Twitch a game
     * carries no channel, and the listener never opens a single connection.
     */
    private val chatVoting = TwitchChatVoting(
        reader = chatReader,
        scope = scope,
        pictures = viewerPictures(),
        board = chatVotes,
        live = ChatVoteBroadcaster(connections),
    )

    /**
     * The viewers writing cards. It keeps its own chat connection: the vote only listens
     * while a round is being judged, and a chat writing cards matters most in the lobby.
     */
    private val chatCards = TwitchChatCards(chatReader, scope) { code, command ->
        games.dispatch(code, command)
    }

    /** The faces under the answers; without Twitch credentials, voters keep their names. */
    private fun viewerPictures(): ViewerPictures =
        if (config.twitch.enabled) TwitchViewers(twitchClient, twitchTokens) else ViewerPictures.NONE

    init {
        games.addListener(GameBroadcaster(connections, views))
        games.addListener(StatsRecorder(statsWriter))
        games.addListener(scheduler)
        games.addListener(idleReaper)
        games.addListener(chatVoting)
        games.addListener(chatCards)
        games.addListener(chatVotes)
        games.addListener(twitchChannels)
        // The deck store follows the games it holds decks for, so an entry never outlives
        // its table — whichever way that table died.
        games.addListener(appliedDecks)
        registerGauges()
    }

    /**
     * What a running instance can be asked about itself. These are the maps that used to
     * have no witness: a leak in any of them shows up here as a number that climbs and
     * never comes back down.
     */
    private fun registerGauges() {
        gauge("cardgame.games.active", "Games with at least one socket watching") {
            connections.activeGames().toDouble()
        }
        gauge("cardgame.players.connected", "Sockets currently open on a game") {
            connections.connectedPlayers().toDouble()
        }
        gauge("cardgame.timers.pending", "Phase timers waiting to fire") { scheduler.size.toDouble() }
        gauge("cardgame.decks.held", "Decks remembered per game") { appliedDecks.size.toDouble() }
        gauge("cardgame.chatvotes.live", "Games holding a running chat tally") {
            chatVotes.size.toDouble()
        }
        gauge("cardgame.twitch.channels", "Twitch channels mapped to a table") {
            twitchChannels.size.toDouble()
        }
    }

    private fun gauge(name: String, help: String, read: () -> Double) {
        Gauge.builder(name) { read() }.description(help).register(metrics)
    }

    private companion object {
        const val IDLE_GAME_MILLIS = 30L * 60 * 1000
    }

    fun close() {
        redis?.close()
        httpClient.close()
    }
}
