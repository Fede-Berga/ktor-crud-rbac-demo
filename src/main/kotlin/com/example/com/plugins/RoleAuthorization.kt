package com.example.com.plugins

import com.example.com.common.ForbiddenException
import com.example.com.domain.Role
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

/**
 * Registers [path]+[method] as its own routing node (distinct from any sibling `get`/`post`
 * handlers on the same [path]) and requires the caller's JWT `role` claim to be one of [roles]
 * before running [body]. The check runs inside the handler itself (not as a separate plugin hook)
 * because JWT authentication populates the principal in a pipeline phase that runs after the
 * `Plugins` phase a route-scoped plugin's `onCall` would otherwise be limited to.
 */
fun Route.roleProtected(
    path: String,
    method: HttpMethod,
    vararg roles: Role,
    body: suspend RoutingContext.() -> Unit,
) {
    val allowedRoles = roles.toSet()
    route(path, method) {
        handle {
            val principal = call.principal<UserPrincipal>()
            if (principal == null || principal.role !in allowedRoles) {
                throw ForbiddenException("Requires role ${allowedRoles.joinToString(" or ")}")
            }
            body()
        }
    }
}
