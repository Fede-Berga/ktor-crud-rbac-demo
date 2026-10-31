package com.example.com.domain

import org.jetbrains.exposed.v1.core.dao.id.UIntIdTable

object Users : UIntIdTable("users") {
    val username = varchar("username", 50).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val role = enumerationByName("role", 20, Role::class)
}

data class User(
    val id: UInt,
    val username: String,
    val passwordHash: String,
    val role: Role,
)
