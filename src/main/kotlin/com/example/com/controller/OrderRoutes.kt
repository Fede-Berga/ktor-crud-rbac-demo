package com.example.com.controller

import com.example.com.common.NotFoundException
import com.example.com.dto.request.OrderRequest
import com.example.com.dto.request.toDomain
import com.example.com.dto.response.toResponse
import com.example.com.plugins.UserPrincipal
import com.example.com.service.OrderService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.orderRoutes() {
    val orderService by inject<OrderService>()

    authenticate("auth-jwt") {
        post("/orders") {
            val principal = call.principal<UserPrincipal>()!!
            val request = call.receive<OrderRequest>()
            val order = orderService.create(request.toDomain(userId = principal.userId))
            call.respond(HttpStatusCode.Created, order.toResponse())
        }

        get("/orders") {
            val principal = call.principal<UserPrincipal>()!!
            val orders = orderService.listFor(principal.userId, principal.role)
            call.respond(orders.map { it.toResponse() })
        }

        get("/orders/{id}") {
            val principal = call.principal<UserPrincipal>()!!
            val id = call.parameters["id"]?.toUIntOrNull() ?: throw NotFoundException("Invalid order id")
            val order = orderService.getById(id, principal.userId, principal.role)
            call.respond(order.toResponse())
        }

        delete("/orders/{id}") {
            val principal = call.principal<UserPrincipal>()!!
            val id = call.parameters["id"]?.toUIntOrNull() ?: throw NotFoundException("Invalid order id")
            orderService.delete(id, principal.userId, principal.role)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
