package com.example.com.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.com.domain.Role
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands

@OptIn(ExperimentalLettuceCoroutinesApi::class)
fun Application.configureSecurity(redis: RedisCoroutinesCommands<String, String>): JwtConfig {
    val jwtConfig = JwtConfig(
        secret = environment.config.property("jwt.secret").getString(),
        issuer = environment.config.property("jwt.issuer").getString(),
        audience = environment.config.property("jwt.audience").getString(),
        realm = environment.config.property("jwt.realm").getString(),
    )

    authentication {
        jwt("auth-jwt") {
            realm = jwtConfig.realm
            verifier(
                JWT.require(Algorithm.HMAC256(jwtConfig.secret))
                    .withAudience(jwtConfig.audience)
                    .withIssuer(jwtConfig.issuer)
                    .build()
            )
            validate { credential ->
                val jti = credential.payload.id ?: return@validate null
                if (redis.get(blacklistKey(jti)) != null) return@validate null

                UserPrincipal(
                    userId = credential.payload.getClaim("userId").asString().toUInt(),
                    username = credential.payload.getClaim("username").asString(),
                    role = Role.valueOf(credential.payload.getClaim("role").asString()),
                    jti = jti,
                    expiresAt = credential.payload.expiresAt.time,
                )
            }
        }
    }

    return jwtConfig
}
