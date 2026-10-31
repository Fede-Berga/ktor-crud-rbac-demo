package com.example.com.dto.request

import com.example.com.domain.Order
import com.example.com.domain.OrderStatus
import kotlinx.serialization.Serializable

@Serializable
data class OrderRequest(val pizzaId: UInt, val quantity: Int)

fun OrderRequest.toDomain(userId: UInt, id: UInt = 0u): Order = Order(
    id = id,
    userId = userId,
    pizzaId = pizzaId,
    quantity = quantity,
    status = OrderStatus.PENDING,
    createdAt = System.currentTimeMillis(),
)
