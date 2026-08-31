package fr.ftnl.cardgame.auth

import io.ktor.server.application.ApplicationCall
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import java.util.UUID

/**
 * Returns the identity of the browser, minting one on first contact. This is what lets a
 * player refresh the page, or lose their network, and still find their seat.
 */
fun ApplicationCall.playerSession(): PlayerSession =
    sessions.get<PlayerSession>() ?: PlayerSession(playerId = UUID.randomUUID().toString())
        .also { sessions.set(it) }

fun ApplicationCall.adminSession(): AdminSession? = sessions.get<AdminSession>()
