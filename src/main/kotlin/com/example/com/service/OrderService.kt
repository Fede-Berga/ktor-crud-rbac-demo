package com.example.com.service

import com.example.com.common.ForbiddenException
import com.example.com.common.NotFoundException
import com.example.com.domain.Order
import com.example.com.domain.Role
import com.example.com.plugins.AppMetrics
import com.example.com.repository.OrderRepository
import com.example.com.repository.PizzaRepository

class OrderService(
    private val orderRepository: OrderRepository,
    private val pizzaRepository: PizzaRepository,
    private val metrics: AppMetrics,
) {
    suspend fun create(order: Order): Order {
        pizzaRepository.findById(order.pizzaId) ?: throw NotFoundException("Pizza ${order.pizzaId} not found")
        val created = orderRepository.create(order)
        metrics.ordersCreated.increment()
        return created
    }

    suspend fun listFor(userId: UInt, role: Role): List<Order> =
        if (role == Role.ADMIN) orderRepository.findAll() else orderRepository.findByUserId(userId)

    suspend fun getById(id: UInt, userId: UInt, role: Role): Order {
        val order = orderRepository.findById(id) ?: throw NotFoundException("Order $id not found")
        if (role != Role.ADMIN && order.userId != userId) throw ForbiddenException("Not your order")
        return order
    }

    suspend fun delete(id: UInt, userId: UInt, role: Role) {
        val order = getById(id, userId, role)
        orderRepository.delete(order.id)
    }
}
