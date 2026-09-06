package fr.ftnl.cardgame.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

/**
 * What the server can be asked about itself.
 *
 * Until now a running instance said nothing but its log lines: no way to see how many
 * tables are live, how long a command takes, or whether the per game maps that hold locks
 * and decks actually come back down. The gauges are registered by whoever owns the number
 * — see [fr.ftnl.cardgame.ApplicationServices] — and read on every scrape.
 */
fun Application.configureMetrics(registry: PrometheusMeterRegistry) {
    install(MicrometerMetrics) {
        this.registry = registry
        meterBinders = listOf(JvmMemoryMetrics(), JvmThreadMetrics(), ProcessorMetrics())
    }
}

fun prometheusRegistry(): PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
