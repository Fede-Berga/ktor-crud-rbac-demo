package com.example.com.dto.response

import com.example.com.domain.Pizza
import kotlinx.serialization.Serializable

@Serializable
data class PizzaResponse(val id: UInt, val name: String, val ingredients: String, val priceCents: Int)

fun Pizza.toResponse(): PizzaResponse = PizzaResponse(id = id, name = name, ingredients = ingredients, priceCents = priceCents)
