package com.example.com.config

import com.example.com.domain.Orders
import com.example.com.domain.Pizzas
import com.example.com.domain.Users
import io.ktor.server.application.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

suspend fun Application.configureDatabase(): R2dbcDatabase {
    val url = environment.config.property("postgres.url").getString()
    val user = environment.config.property("postgres.user").getString()
    val password = environment.config.property("postgres.password").getString()

    val database = R2dbcDatabase.connect(url = url, user = user, password = password)

    suspendTransaction(database) {
        SchemaUtils.create(Users, Pizzas, Orders)
    }

    return database
}
