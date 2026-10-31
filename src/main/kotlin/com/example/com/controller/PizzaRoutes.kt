package com.example.com.controller

import com.example.com.common.NotFoundException
import com.example.com.domain.Role
import com.example.com.dto.request.PizzaRequest
import com.example.com.dto.response.toResponse
import com.example.com.plugins.roleProtected
import com.example.com.service.PizzaService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.pizzaRoutes() {
    val pizzaService by inject<PizzaService>()

    authenticate("auth-jwt") {
        get("/pizzas") {
            call.respond(pizzaService.findAll().map { it.toResponse() })
        }

        get("/pizzas/{id}") {
            val id = call.parameters["id"]?.toUIntOrNull() ?: throw NotFoundException("Invalid pizza id")
            val pizza = pizzaService.findById(id) ?: throw NotFoundException("Pizza $id not found")
            call.respond(pizza.toResponse())
        }

        roleProtected("/pizzas", HttpMethod.Post, Role.ADMIN) {
            val request = call.receive<PizzaRequest>()
            val pizza = pizzaService.create(request.name, request.ingredients, request.priceCents)
            call.respond(HttpStatusCode.Created, pizza.toResponse())
        }

        roleProtected("/pizzas/{id}", HttpMethod.Put, Role.ADMIN) {
            val id = call.parameters["id"]?.toUIntOrNull() ?: throw NotFoundException("Invalid pizza id")
            val request = call.receive<PizzaRequest>()
            val pizza = pizzaService.update(id, request.name, request.ingredients, request.priceCents)
                ?: throw NotFoundException("Pizza $id not found")
            call.respond(pizza.toResponse())
        }

        roleProtected("/pizzas/{id}", HttpMethod.Delete, Role.ADMIN) {
            val id = call.parameters["id"]?.toUIntOrNull() ?: throw NotFoundException("Invalid pizza id")
            if (!pizzaService.delete(id)) throw NotFoundException("Pizza $id not found")
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
