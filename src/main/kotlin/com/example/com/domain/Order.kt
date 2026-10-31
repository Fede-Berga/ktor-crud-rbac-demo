package com.example.com.domain

import org.jetbrains.exposed.v1.core.dao.id.UIntIdTable

enum class OrderStatus { PENDING, CONFIRMED, DELIVERED, CANCELLED }

object Orders : UIntIdTable("orders") {
    val userId = reference("user_id", Users)
    val pizzaId = reference("pizza_id", Pizzas)
    val quantity = integer("quantity")
    val status = enumerationByName("status", 20, OrderStatus::class)
    val createdAt = long("created_at")
}

data class Order(
    val id: UInt,
    val userId: UInt,
    val pizzaId: UInt,
    val quantity: Int,
    val status: OrderStatus,
    val createdAt: Long,
)
