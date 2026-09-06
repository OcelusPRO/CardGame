package fr.ftnl.cardgame.api

import fr.ftnl.cardgame.config.MetricsConfig
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

/**
 * The Prometheus scrape endpoint. It carries counters, timings and gauges — no player, no
 * nickname, no game code — so it is open unless a token says otherwise.
 */
fun Route.metricsRoutes(registry: PrometheusMeterRegistry, config: MetricsConfig) {
    get("/metrics") {
        if (config.guarded && call.bearer() != config.token) {
            return@get call.respondText("", status = HttpStatusCode.Unauthorized)
        }
        call.respondText(registry.scrape(), ContentType.Text.Plain)
    }
}

private fun io.ktor.server.application.ApplicationCall.bearer(): String? =
    request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")?.trim()
