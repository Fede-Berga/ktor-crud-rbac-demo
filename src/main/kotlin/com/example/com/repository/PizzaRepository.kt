package com.example.com.repository

import com.example.com.domain.Pizza
import com.example.com.domain.Pizzas
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

class PizzaRepository(private val database: R2dbcDatabase) {

    suspend fun findAll(): List<Pizza> = suspendTransaction(database) {
        Pizzas.selectAll().map { it.toPizza() }.toList()
    }

    suspend fun findById(id: UInt): Pizza? = suspendTransaction(database) {
        Pizzas.selectAll().where { Pizzas.id eq id }
            .map { it.toPizza() }
            .singleOrNull()
    }

    suspend fun create(name: String, ingredients: String, priceCents: Int): Pizza = suspendTransaction(database) {
        val id = Pizzas.insert {
            it[Pizzas.name] = name
            it[Pizzas.ingredients] = ingredients
            it[Pizzas.priceCents] = priceCents
        }[Pizzas.id].value
        Pizza(id = id, name = name, ingredients = ingredients, priceCents = priceCents)
    }

    suspend fun update(id: UInt, name: String, ingredients: String, priceCents: Int): Pizza? = suspendTransaction(database) {
        val updated = Pizzas.update({ Pizzas.id eq id }) {
            it[Pizzas.name] = name
            it[Pizzas.ingredients] = ingredients
            it[Pizzas.priceCents] = priceCents
        }
        if (updated > 0) Pizza(id = id, name = name, ingredients = ingredients, priceCents = priceCents) else null
    }

    suspend fun delete(id: UInt): Boolean = suspendTransaction(database) {
        Pizzas.deleteWhere { Pizzas.id eq id } > 0
    }
}

private fun ResultRow.toPizza() = Pizza(
    id = this[Pizzas.id].value,
    name = this[Pizzas.name],
    ingredients = this[Pizzas.ingredients],
    priceCents = this[Pizzas.priceCents],
)
