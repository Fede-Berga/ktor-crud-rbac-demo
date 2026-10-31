package com.example.com.repository

import com.example.com.domain.Order
import com.example.com.domain.Orders
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

class OrderRepository(private val database: R2dbcDatabase) {

    suspend fun findAll(): List<Order> = suspendTransaction(database) {
        Orders.selectAll().map { it.toOrder() }.toList()
    }

    suspend fun findByUserId(userId: UInt): List<Order> = suspendTransaction(database) {
        Orders.selectAll().where { Orders.userId eq userId }
            .map { it.toOrder() }
            .toList()
    }

    suspend fun findById(id: UInt): Order? = suspendTransaction(database) {
        Orders.selectAll().where { Orders.id eq id }
            .map { it.toOrder() }
            .singleOrNull()
    }

    suspend fun create(order: Order): Order = suspendTransaction(database) {
        val id = Orders.insert {
            it[Orders.userId] = order.userId
            it[Orders.pizzaId] = order.pizzaId
            it[Orders.quantity] = order.quantity
            it[Orders.status] = order.status
            it[Orders.createdAt] = order.createdAt
        }[Orders.id].value
        order.copy(id = id)
    }

    suspend fun delete(id: UInt): Boolean = suspendTransaction(database) {
        Orders.deleteWhere { Orders.id eq id } > 0
    }
}

private fun ResultRow.toOrder() = Order(
    id = this[Orders.id].value,
    userId = this[Orders.userId].value,
    pizzaId = this[Orders.pizzaId].value,
    quantity = this[Orders.quantity],
    status = this[Orders.status],
    createdAt = this[Orders.createdAt],
)
