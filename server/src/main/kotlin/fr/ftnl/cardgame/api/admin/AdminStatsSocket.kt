package fr.ftnl.cardgame.api.admin

import fr.ftnl.cardgame.api.dto.LiveStatsView
import fr.ftnl.cardgame.auth.adminSession
import fr.ftnl.cardgame.stats.StatsService
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import io.ktor.websocket.send
import io.ktor.server.routing.Route
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json

/** Pushes the live counters to the dashboard, which is what makes its charts move. */
fun Route.adminStatsSocket(stats: StatsService, json: Json) {
    webSocket("/ws/admin/stats") {
        if (call.adminSession() == null) {
            return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Réservé aux administrateurs"))
        }
        while (isActive) {
            send(json.encodeToString(LiveStatsView.serializer(), stats.live()))
            delay(REFRESH_MILLIS)
        }
    }
}

private const val REFRESH_MILLIS = 2_000L
