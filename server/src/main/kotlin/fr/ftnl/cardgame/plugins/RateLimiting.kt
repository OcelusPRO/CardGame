package fr.ftnl.cardgame.plugins

import fr.ftnl.cardgame.config.HttpConfig
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import kotlin.time.Duration.Companion.minutes

/** Opening a table: cheap for the caller, a code plus a deck query for the server. */
val CREATE_GAMES = RateLimitName("create-games")

/** Sitting down at one, which the invitation link makes a normal thing to retry. */
val JOIN_GAMES = RateLimitName("join-games")

/**
 * Caps what a single caller can ask of the unauthenticated surface.
 *
 * Creating a table allocates a code, resolves a deck out of the catalogue and writes a
 * snapshot to Redis — all of it behind no sign in at all, which made a shell loop enough
 * to fill the store and drain the connection pool. The socket needs no cap of its own:
 * it is only reachable from a seat, and every command on it is refused by the domain when
 * it is not the player's to send.
 */
fun Application.configureRateLimiting(config: HttpConfig) {
    if (config.trustProxyHeaders) install(XForwardedHeaders)
    install(RateLimit) {
        register(CREATE_GAMES) {
            rateLimiter(limit = CREATE_LIMIT, refillPeriod = WINDOW)
            requestKey { call -> call.callerKey() }
        }
        register(JOIN_GAMES) {
            rateLimiter(limit = JOIN_LIMIT, refillPeriod = WINDOW)
            requestKey { call -> call.callerKey() }
        }
    }
}

/**
 * Who is being counted. A whole flat sharing one address is a normal table, so the window
 * is wide enough to seat them; it is the thousand-a-minute script it exists to stop.
 */
private fun ApplicationCall.callerKey(): String = request.origin.remoteAddress

private val WINDOW = 10.minutes
private const val CREATE_LIMIT = 30
private const val JOIN_LIMIT = 120
