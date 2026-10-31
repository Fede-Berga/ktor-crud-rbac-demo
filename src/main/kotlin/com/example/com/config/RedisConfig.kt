package com.example.com.config

import io.ktor.server.application.*
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands

@OptIn(ExperimentalLettuceCoroutinesApi::class)
fun Application.configureRedis(): RedisCoroutinesCommands<String, String> {
    val url = environment.config.property("redis.url").getString()
    val client = RedisClient.create(url)
    val connection = client.connect()
    return connection.coroutines()
}
