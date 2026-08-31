package fr.ftnl.cardgame

import fr.ftnl.cardgame.config.AppConfigLoader
import fr.ftnl.cardgame.db.DatabaseFactory
import fr.ftnl.cardgame.plugins.configureHttp
import fr.ftnl.cardgame.plugins.configureRouting
import fr.ftnl.cardgame.plugins.configureSecurity
import fr.ftnl.cardgame.plugins.configureSerialization
import fr.ftnl.cardgame.plugins.configureSockets
import fr.ftnl.cardgame.plugins.configureStatusPages
import fr.ftnl.cardgame.seed.DevDeckSeeder
import io.ktor.server.application.Application
import io.ktor.server.application.log
import kotlinx.coroutines.launch

/** Entry point declared in `application.yaml`. */
fun Application.module() {
    val config = AppConfigLoader.load(environment.config)
    DatabaseFactory.connect(config.database)
    val services = ApplicationServices(config, this, HttpClientFactory.create())
    monitor.subscribe(io.ktor.server.application.ApplicationStopped) { services.close() }
    configure(services)
    if (config.seed.enabled) launch { seedDemoDeck(services) }
}

/** Installs every plugin and route; tests call this with their own services. */
fun Application.configure(services: ApplicationServices) {
    configureSerialization()
    configureHttp()
    configureStatusPages()
    configureSockets()
    configureSecurity(services.config, services.httpClient)
    configureRouting(services)
}

private suspend fun Application.seedDemoDeck(services: ApplicationServices) {
    val seeder = DevDeckSeeder(
        packs = services.packRepository,
        situations = services.situationRepository,
        punchlines = services.punchlineRepository,
        clock = services.clock,
    )
    if (seeder.seed()) log.info("Demo deck loaded; disable it with SEED_DEV_DECK=false")
}
