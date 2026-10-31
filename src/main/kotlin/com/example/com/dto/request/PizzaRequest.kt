package com.example.com.dto.request

import com.example.com.domain.Pizza
import kotlinx.serialization.Serializable

@Serializable
data class PizzaRequest(val name: String, val ingredients: String, val priceCents: Int)

fun PizzaRequest.toDomain(id: UInt = 0u): Pizza =
    Pizza(id = id, name = name, ingredients = ingredients, priceCents = priceCents)
