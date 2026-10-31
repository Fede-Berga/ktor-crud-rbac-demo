package com.example.com.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.com.domain.Role
import java.util.Date
import java.util.UUID

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    val tokenTtlMillis: Long = 15 * 60 * 1000,
)

data class UserPrincipal(
    val userId: UInt,
    val username: String,
    val role: Role,
    val jti: String,
    val expiresAt: Long,
)

data class IssuedToken(val token: String, val jti: String, val expiresAt: Long)

fun blacklistKey(jti: String): String = "jwt:blacklist:$jti"

class TokenService(private val jwtConfig: JwtConfig) {

    fun issueToken(userId: UInt, username: String, role: Role): IssuedToken {
        val jti = UUID.randomUUID().toString()
        val expiresAt = System.currentTimeMillis() + jwtConfig.tokenTtlMillis
        val token = JWT.create()
            .withAudience(jwtConfig.audience)
            .withIssuer(jwtConfig.issuer)
            .withJWTId(jti)
            .withClaim("userId", userId.toString())
            .withClaim("username", username)
            .withClaim("role", role.name)
            .withExpiresAt(Date(expiresAt))
            .sign(Algorithm.HMAC256(jwtConfig.secret))
        return IssuedToken(token, jti, expiresAt)
    }
}
