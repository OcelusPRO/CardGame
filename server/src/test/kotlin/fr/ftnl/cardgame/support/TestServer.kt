package fr.ftnl.cardgame.support

import fr.ftnl.cardgame.ApplicationServices
import fr.ftnl.cardgame.HttpClientFactory
import fr.ftnl.cardgame.config.AppConfig
import fr.ftnl.cardgame.session.InMemoryGameSessionStore
import fr.ftnl.cardgame.auth.AdminSession
import fr.ftnl.cardgame.auth.playerSession
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import fr.ftnl.cardgame.configure
import fr.ftnl.cardgame.plugins.ApiJson

/**
 * Boots the real application against the PostgreSQL test container and an in-memory
 * session store, so an integration test exercises the true routing, serialisation,
 * session handling and SQL.
 */
fun ApplicationTestBuilder.startTestServer(
    appConfig: AppConfig = TestConfig.create(),
): ApplicationServices {
    TestDatabase.connect()
    val services = ApplicationServices(
        config = appConfig,
        scope = testScope(),
        httpClient = HttpClientFactory.create(),
        sessionStore = InMemoryGameSessionStore(),
    )
    environment { config = MapApplicationConfig() }
    application {
        configure(services)
        routing {
            // Test-only door. The real one is the Discord OAuth callback, which cannot run
            // here, so this hands out the very same signed session the callback would set.
            get(ADMIN_LOGIN_PATH) {
                call.sessions.set(
                    call.playerSession().copy(
                        discordId = TestConfig.ADMIN_DISCORD_ID,
                        discordUsername = "Root",
                    )
                )
                call.sessions.set(AdminSession("DISCORD", TestConfig.ADMIN_DISCORD_ID, "Root"))
                call.respond(HttpStatusCode.NoContent)
            }
            // Test-only Twitch sign in: `?login=kameto` picks the channel that would be
            // read from the real callback.
            get(TWITCH_LOGIN_PATH) {
                val login = call.request.queryParameters["login"] ?: "kameto"
                val id = call.request.queryParameters["id"] ?: "200000000000000001"
                call.sessions.set(
                    call.playerSession().copy(
                        twitchId = id,
                        twitchLogin = login,
                        twitchUsername = login,
                        twitchCreatedAtMillis = call.request.queryParameters["createdAt"]?.toLongOrNull(),
                    )
                )
                call.respond(HttpStatusCode.NoContent)
            }
            // Test-only Discord sign in for a non-admin account: `?id=123` picks the id.
            get(DISCORD_LOGIN_PATH) {
                val id = call.request.queryParameters["id"] ?: "100000000000000001"
                call.sessions.set(
                    call.playerSession().copy(discordId = id, discordUsername = "User $id")
                )
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
    return services
}

const val ADMIN_LOGIN_PATH = "/test/sign-in-as-admin"
const val DISCORD_LOGIN_PATH = "/test/sign-in-with-discord"
const val TWITCH_LOGIN_PATH = "/test/sign-in-with-twitch"

/** A browser that carries an administrator session from its very first call. */
suspend fun ApplicationTestBuilder.adminBrowser(): HttpClient =
    browser().also { it.get(ADMIN_LOGIN_PATH) }

/** A browser-like client: it keeps its cookies, so it stays the same player. */
fun ApplicationTestBuilder.browser(): HttpClient = createClient {
    install(ContentNegotiation) { json(ApiJson) }
    install(HttpCookies)
    install(WebSockets)
    install(HttpTimeout) { requestTimeoutMillis = 20_000 }
}

private fun ApplicationTestBuilder.testScope() = kotlinx.coroutines.CoroutineScope(
    kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default
)
