package com.example.com.domain

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.dao.id.UIntIdTable

object Pizzas : UIntIdTable("pizzas") {
    val name = varchar("name", 100)
    val ingredients = varchar("ingredients", 500)
    val priceCents = integer("price_cents")
}

@Serializable
data class Pizza(
    val id: UInt,
    val name: String,
    val ingredients: String,
    val priceCents: Int,
)
