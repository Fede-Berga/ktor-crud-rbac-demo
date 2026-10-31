package com.example.com

import com.example.com.config.configureDatabase
import com.example.com.config.configureRedis
import com.example.com.controller.authRoutes
import com.example.com.controller.orderRoutes
import com.example.com.controller.pizzaRoutes
import com.example.com.plugins.TokenService
import com.example.com.plugins.configureDependencyInjection
import com.example.com.plugins.configureMonitoring
import com.example.com.plugins.configureSecurity
import com.example.com.plugins.configureSerialization
import com.example.com.plugins.configureStatusPages
import com.example.com.service.AuthService
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import org.koin.ktor.ext.inject

@OptIn(ExperimentalLettuceCoroutinesApi::class)
suspend fun Application.module() {
    configureSerialization()
    configureStatusPages()
    val metrics = configureMonitoring()
    val database = configureDatabase()
    val redis = configureRedis()
    val jwtConfig = configureSecurity(redis)
    val tokenService = TokenService(jwtConfig)
    configureDependencyInjection(database, redis, metrics, tokenService)

    routing {
        authRoutes()
        pizzaRoutes()
        orderRoutes()
    }

    val authService by inject<AuthService>()
    authService.seedDemoUsersIfEmpty()
}
