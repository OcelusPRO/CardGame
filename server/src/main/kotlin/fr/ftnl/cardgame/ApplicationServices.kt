package fr.ftnl.cardgame

import fr.ftnl.cardgame.api.view.GameViewFactory
import fr.ftnl.cardgame.auth.AdminGuard
import fr.ftnl.cardgame.auth.DiscordClient
import fr.ftnl.cardgame.catalog.AdminCardService
import fr.ftnl.cardgame.catalog.AdminPackService
import fr.ftnl.cardgame.catalog.CardPackRepository
import fr.ftnl.cardgame.catalog.CardPoolResolver
import fr.ftnl.cardgame.catalog.CatalogService
import fr.ftnl.cardgame.catalog.CustomCardFactory
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
import fr.ftnl.cardgame.game.GameEntryService
import fr.ftnl.cardgame.game.GameFactory
import fr.ftnl.cardgame.game.GameLocks
import fr.ftnl.cardgame.game.GameService
import fr.ftnl.cardgame.game.PhaseScheduler
import fr.ftnl.cardgame.plugins.ApiJson
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
import fr.ftnl.cardgame.ws.GameBroadcaster
import fr.ftnl.cardgame.ws.GameCommandTranslator
import fr.ftnl.cardgame.ws.GameConnections
import fr.ftnl.cardgame.ws.GameSocketHandler
import io.ktor.client.HttpClient
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
) {
    private val redis: JedisPooled? =
        if (sessionStore == null && config.redis.enabled) JedisPooled(config.redis.url) else null

    val connections = GameConnections()

    val packRepository: CardPackRepository = ExposedCardPackRepository()
    val situationRepository: SituationCardRepository = ExposedSituationCardRepository()
    val punchlineRepository: PunchlineCardRepository = ExposedPunchlineCardRepository()
    private val statsWriter: UsageStatsWriter = ExposedUsageStatsWriter()
    private val statsReader: UsageStatsReader = ExposedUsageStatsReader()

    val catalog = CatalogService(packRepository, situationRepository, punchlineRepository)
    val adminPacks = AdminPackService(packRepository, situationRepository, punchlineRepository, clock)
    val adminCards = AdminCardService(situationRepository, punchlineRepository, clock)
    private val deckResolver = CardPoolResolver(packRepository, situationRepository, punchlineRepository)

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

    val views = GameViewFactory(clock)
    val entry = GameEntryService(games, deckResolver)
    val statsService = StatsService(statsReader, packRepository, situationRepository, punchlineRepository, connections, clock)
    val discordClient = DiscordClient(httpClient)
    val adminGuard = AdminGuard(config.admin)

    val socketHandler = GameSocketHandler(
        games = games,
        connections = connections,
        views = views,
        translator = GameCommandTranslator(deckResolver, CustomCardFactory()),
        json = ApiJson,
    )

    private val scheduler = PhaseScheduler(scope, clock) { code, command -> games.dispatch(code, command) }

    init {
        games.addListener(GameBroadcaster(connections, views))
        games.addListener(StatsRecorder(statsWriter))
        games.addListener(scheduler)
    }

    fun close() {
        redis?.close()
        httpClient.close()
    }
}
