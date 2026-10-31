package com.example.com.plugins

import com.example.com.repository.OrderRepository
import com.example.com.repository.PizzaRepository
import com.example.com.repository.UserRepository
import com.example.com.service.AuthService
import com.example.com.service.OrderService
import com.example.com.service.PizzaService
import io.ktor.server.application.*
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

@OptIn(ExperimentalLettuceCoroutinesApi::class)
fun Application.configureDependencyInjection(
    database: R2dbcDatabase,
    redis: RedisCoroutinesCommands<String, String>,
    metrics: AppMetrics,
    tokenService: TokenService,
) {
    install(Koin) {
        slf4jLogger()
        modules(module {
            single { database }
            single { redis }
            single { metrics }
            single { tokenService }
            single { UserRepository(get()) }
            single { PizzaRepository(get()) }
            single { OrderRepository(get()) }
            single { AuthService(get(), get(), get(), get()) }
            single { PizzaService(get(), get(), get()) }
            single { OrderService(get(), get(), get()) }
        })
    }
}
