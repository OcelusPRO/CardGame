package fr.ftnl.cardgame.plugins

import fr.ftnl.cardgame.ApplicationServices
import fr.ftnl.cardgame.api.admin.adminAccountRoutes
import fr.ftnl.cardgame.api.admin.adminAdultAccessRoutes
import fr.ftnl.cardgame.api.admin.adminCardRoutes
import fr.ftnl.cardgame.api.admin.adminPackRoutes
import fr.ftnl.cardgame.api.admin.adminStatsRoutes
import fr.ftnl.cardgame.api.admin.adminStatsSocket
import fr.ftnl.cardgame.api.authRoutes
import fr.ftnl.cardgame.api.cardRoutes
import fr.ftnl.cardgame.api.discordAuthRoutes
import fr.ftnl.cardgame.api.gameRoutes
import fr.ftnl.cardgame.api.healthRoutes
import fr.ftnl.cardgame.api.metricsRoutes
import fr.ftnl.cardgame.api.spaRoutes
import fr.ftnl.cardgame.api.twitchAuthRoutes
import fr.ftnl.cardgame.api.twitchExtensionRoutes
import fr.ftnl.cardgame.api.twitchRewardRoutes
import fr.ftnl.cardgame.ws.gameSocketRoute
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

/** The whole surface of the server, in one readable list. */
fun Application.configureRouting(services: ApplicationServices) {
    routing {
        healthRoutes()
        metricsRoutes(services.metrics, services.config.metrics)
        authRoutes(
            services.config.discord,
            services.config.twitch,
            services.config.twitchExtension,
            services.twitchHostTokens,
        )
        if (services.config.discord.enabled) {
            discordAuthRoutes(services.discordClient, services.adminGuard)
        }
        if (services.config.twitch.enabled) {
            twitchAuthRoutes(services.twitchClient, services.adminGuard, services.twitchHostTokens)
            twitchRewardRoutes(services.twitchHostTokens, services.rewardCards, services.twitchRewards)
        }
        if (services.config.twitchExtension.enabled) {
            twitchExtensionRoutes(
                services.config.twitchExtension,
                services.extensionTokens,
                services.extensionCards,
            )
        }
        gameRoutes(services.entry)
        cardRoutes(services.catalog, services.adultAccessGuard, services.appliedDecks)
        adminPackRoutes(services.adminPacks)
        adminCardRoutes(services.adminCards)
        adminAdultAccessRoutes(services.adminAdultAccess)
        adminAccountRoutes(services.accounts)
        adminStatsRoutes(services.statsService)
        adminStatsSocket(services.statsService, ApiJson)
        gameSocketRoute(services.games, services.socketHandler)
        spaRoutes()
    }
}
