package com.example.com.plugins

import com.example.com.common.ForbiddenException
import com.example.com.common.InvalidCredentialsException
import com.example.com.common.NotFoundException
import com.example.com.common.TooManyRequestsException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(val error: String)

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<NotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse(cause.message ?: "Not found"))
        }
        exception<ForbiddenException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden, ErrorResponse(cause.message ?: "Forbidden"))
        }
        exception<InvalidCredentialsException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse(cause.message ?: "Invalid credentials"))
        }
        exception<TooManyRequestsException> { call, cause ->
            call.respond(HttpStatusCode.TooManyRequests, ErrorResponse(cause.message ?: "Too many requests"))
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Bad request"))
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Internal server error"))
        }
    }
}
