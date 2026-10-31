package com.example.com.dto.response

import com.example.com.domain.Order
import com.example.com.domain.OrderStatus
import kotlinx.serialization.Serializable

@Serializable
data class OrderResponse(
    val id: UInt,
    val userId: UInt,
    val pizzaId: UInt,
    val quantity: Int,
    val status: OrderStatus,
    val createdAt: Long,
)

fun Order.toResponse(): OrderResponse = OrderResponse(
    id = id,
    userId = userId,
    pizzaId = pizzaId,
    quantity = quantity,
    status = status,
    createdAt = createdAt,
)
