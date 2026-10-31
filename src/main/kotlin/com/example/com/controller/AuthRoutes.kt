package com.example.com.controller

import com.example.com.dto.request.LoginRequest
import com.example.com.dto.response.LoginResponse
import com.example.com.plugins.UserPrincipal
import com.example.com.service.AuthService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.origin
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.authRoutes() {
    val authService by inject<AuthService>()

    post("/auth/login") {
        val request = call.receive<LoginRequest>()
        val clientIp = call.request.origin.remoteHost
        val issued = authService.login(request.username, request.password, clientIp)
        call.respond(HttpStatusCode.OK, LoginResponse(issued.token))
    }

    authenticate("auth-jwt") {
        post("/auth/logout") {
            val principal = call.principal<UserPrincipal>()!!
            authService.logout(principal.jti, principal.expiresAt)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
