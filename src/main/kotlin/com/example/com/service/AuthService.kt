package com.example.com.service

import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.com.common.InvalidCredentialsException
import com.example.com.common.TooManyRequestsException
import com.example.com.domain.Role
import com.example.com.plugins.AppMetrics
import com.example.com.plugins.IssuedToken
import com.example.com.plugins.TokenService
import com.example.com.plugins.blacklistKey
import com.example.com.repository.UserRepository
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands

private const val LOGIN_RATE_LIMIT = 5
private const val LOGIN_RATE_LIMIT_WINDOW_SECONDS = 60L
private fun rateLimitKey(clientIp: String) = "ratelimit:login:$clientIp"

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class AuthService(
    private val userRepository: UserRepository,
    private val tokenService: TokenService,
    private val redis: RedisCoroutinesCommands<String, String>,
    private val metrics: AppMetrics,
) {
    suspend fun seedDemoUsersIfEmpty() {
        if (userRepository.count() > 0) return
        userRepository.create("admin", hash("admin123"), Role.ADMIN)
        userRepository.create("alice", hash("alice123"), Role.USER)
    }

    suspend fun login(username: String, password: String, clientIp: String): IssuedToken {
        checkRateLimit(clientIp)
        val user = userRepository.findByUsername(username) ?: throw InvalidCredentialsException()
        val verified = BCrypt.verifyer().verify(password.toCharArray(), user.passwordHash).verified
        if (!verified) throw InvalidCredentialsException()
        return tokenService.issueToken(user.id, user.username, user.role)
    }

    suspend fun logout(jti: String, expiresAt: Long) {
        val ttlSeconds = ((expiresAt - System.currentTimeMillis()) / 1000).coerceAtLeast(1)
        redis.setex(blacklistKey(jti), ttlSeconds, "revoked")
    }

    private suspend fun checkRateLimit(clientIp: String) {
        val key = rateLimitKey(clientIp)
        val attempts = redis.incr(key) ?: 1
        if (attempts == 1L) redis.expire(key, LOGIN_RATE_LIMIT_WINDOW_SECONDS)
        if (attempts > LOGIN_RATE_LIMIT) {
            metrics.loginRateLimited.increment()
            throw TooManyRequestsException("Too many login attempts, try again later")
        }
    }

    private fun hash(password: String): String = BCrypt.withDefaults().hashToString(12, password.toCharArray())
}
