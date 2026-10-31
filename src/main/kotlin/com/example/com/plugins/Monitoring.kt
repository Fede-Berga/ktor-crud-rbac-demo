package com.example.com.plugins

import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

class AppMetrics(appMicrometerRegistry: PrometheusMeterRegistry) {
    val ordersCreated: Counter = Counter.builder("pizza_orders_created_total")
        .description("Number of pizza orders created")
        .register(appMicrometerRegistry)

    val pizzaCacheHits: Counter = Counter.builder("pizza_cache_requests_total")
        .description("Pizza menu cache lookups")
        .tag("result", "hit")
        .register(appMicrometerRegistry)

    val pizzaCacheMisses: Counter = Counter.builder("pizza_cache_requests_total")
        .description("Pizza menu cache lookups")
        .tag("result", "miss")
        .register(appMicrometerRegistry)

    val loginRateLimited: Counter = Counter.builder("auth_login_rate_limited_total")
        .description("Login attempts rejected by the Redis-backed rate limiter")
        .register(appMicrometerRegistry)
}

fun Application.configureMonitoring(): AppMetrics {
    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    ClassLoaderMetrics().bindTo(appMicrometerRegistry)
    JvmMemoryMetrics().bindTo(appMicrometerRegistry)
    JvmGcMetrics().bindTo(appMicrometerRegistry)
    JvmThreadMetrics().bindTo(appMicrometerRegistry)
    ProcessorMetrics().bindTo(appMicrometerRegistry)
    UptimeMetrics().bindTo(appMicrometerRegistry)

    install(MicrometerMetrics) {
        registry = appMicrometerRegistry
    }

    routing {
        get("/metrics-micrometer") {
            call.respond(appMicrometerRegistry.scrape())
        }
    }

    return AppMetrics(appMicrometerRegistry)
}
