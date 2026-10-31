package com.example.com.repository

import com.example.com.domain.Role
import com.example.com.domain.User
import com.example.com.domain.Users
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

class UserRepository(private val database: R2dbcDatabase) {

    suspend fun findByUsername(username: String): User? = suspendTransaction(database) {
        Users.selectAll().where { Users.username eq username }
            .map { it.toUser() }
            .singleOrNull()
    }

    suspend fun create(username: String, passwordHash: String, role: Role): User = suspendTransaction(database) {
        val id = Users.insert {
            it[Users.username] = username
            it[Users.passwordHash] = passwordHash
            it[Users.role] = role
        }[Users.id].value
        User(id = id, username = username, passwordHash = passwordHash, role = role)
    }

    suspend fun count(): Int = suspendTransaction(database) {
        Users.selectAll().count().toInt()
    }
}

private fun ResultRow.toUser() = User(
    id = this[Users.id].value,
    username = this[Users.username],
    passwordHash = this[Users.passwordHash],
    role = this[Users.role],
)
