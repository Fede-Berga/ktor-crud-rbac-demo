package com.example.com.service

import com.example.com.domain.Pizza
import com.example.com.plugins.AppMetrics
import com.example.com.repository.PizzaRepository
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.serialization.json.Json

private const val CACHE_TTL_SECONDS = 60L
private const val ALL_PIZZAS_KEY = "pizza:all"
private fun pizzaKey(id: UInt) = "pizza:$id"

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class PizzaService(
    private val repository: PizzaRepository,
    private val redis: RedisCoroutinesCommands<String, String>,
    private val metrics: AppMetrics,
) {
    suspend fun findAll(): List<Pizza> {
        redis.get(ALL_PIZZAS_KEY)?.let {
            metrics.pizzaCacheHits.increment()
            return Json.decodeFromString(it)
        }
        metrics.pizzaCacheMisses.increment()
        val pizzas = repository.findAll()
        redis.set(ALL_PIZZAS_KEY, Json.encodeToString(pizzas))
        redis.expire(ALL_PIZZAS_KEY, CACHE_TTL_SECONDS)
        return pizzas
    }

    suspend fun findById(id: UInt): Pizza? {
        redis.get(pizzaKey(id))?.let {
            metrics.pizzaCacheHits.increment()
            return Json.decodeFromString(it)
        }
        metrics.pizzaCacheMisses.increment()
        val pizza = repository.findById(id) ?: return null
        redis.set(pizzaKey(id), Json.encodeToString(pizza))
        redis.expire(pizzaKey(id), CACHE_TTL_SECONDS)
        return pizza
    }

    suspend fun create(name: String, ingredients: String, priceCents: Int): Pizza {
        val pizza = repository.create(name, ingredients, priceCents)
        redis.del(ALL_PIZZAS_KEY)
        return pizza
    }

    suspend fun update(id: UInt, name: String, ingredients: String, priceCents: Int): Pizza? {
        val pizza = repository.update(id, name, ingredients, priceCents) ?: return null
        redis.del(ALL_PIZZAS_KEY, pizzaKey(id))
        return pizza
    }

    suspend fun delete(id: UInt): Boolean {
        val deleted = repository.delete(id)
        if (deleted) redis.del(ALL_PIZZAS_KEY, pizzaKey(id))
        return deleted
    }
}
