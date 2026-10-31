package com.example.com

import com.example.com.dto.request.LoginRequest
import com.example.com.dto.request.OrderRequest
import com.example.com.dto.request.PizzaRequest
import com.example.com.dto.response.LoginResponse
import com.example.com.dto.response.OrderResponse
import com.example.com.dto.response.PizzaResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.lettuce.core.RedisClient
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * These tests hit a real Postgres/Redis (docker-compose). Redis is flushed before every test so
 * the login rate-limiter and JWT blacklist don't leak state between test runs.
 */
class ServerTest {

    @BeforeTest
    @AfterTest
    fun resetRedisState() {
        val client = RedisClient.create("redis://localhost:6379")
        client.connect().use { connection -> connection.sync().flushall() }
        client.shutdown()
    }

    private fun ApplicationTestBuilder.jsonClient(): HttpClient = createClient {
        install(ContentNegotiation) { json() }
    }

    private suspend fun HttpClient.login(username: String, password: String): String {
        val response = post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username, password))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        return response.body<LoginResponse>().token
    }

    @Test
    fun `alice can log in and browse the menu`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }
        val client = jsonClient()
        val token = client.login("alice", "alice123")

        val response = client.get("/pizzas") { bearerAuth(token) }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `user cannot create a pizza but admin can`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }
        val client = jsonClient()

        val userToken = client.login("alice", "alice123")
        val forbidden = client.post("/pizzas") {
            bearerAuth(userToken)
            contentType(ContentType.Application.Json)
            setBody(PizzaRequest("Margherita-${System.nanoTime()}", "tomato, mozzarella", 900))
        }
        assertEquals(HttpStatusCode.Forbidden, forbidden.status)

        val adminToken = client.login("admin", "admin123")
        val created = client.post("/pizzas") {
            bearerAuth(adminToken)
            contentType(ContentType.Application.Json)
            setBody(PizzaRequest("Margherita-${System.nanoTime()}", "tomato, mozzarella", 900))
        }
        assertEquals(HttpStatusCode.Created, created.status)
    }

    @Test
    fun `users only see their own orders, admin sees all`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }
        val client = jsonClient()

        val adminToken = client.login("admin", "admin123")
        val pizzaResponse = client.post("/pizzas") {
            bearerAuth(adminToken)
            contentType(ContentType.Application.Json)
            setBody(PizzaRequest("Diavola-${System.nanoTime()}", "spicy salami", 1100))
        }
        assertEquals(HttpStatusCode.Created, pizzaResponse.status)
        val pizza = pizzaResponse.body<PizzaResponse>()

        val aliceToken = client.login("alice", "alice123")
        val orderResponse = client.post("/orders") {
            bearerAuth(aliceToken)
            contentType(ContentType.Application.Json)
            setBody(OrderRequest(pizza.id, 2))
        }
        assertEquals(HttpStatusCode.Created, orderResponse.status)
        val createdOrder = orderResponse.body<OrderResponse>()

        val aliceOrders = client.get("/orders") { bearerAuth(aliceToken) }.body<List<OrderResponse>>()
        assertTrue(aliceOrders.all { it.userId == createdOrder.userId })
        assertTrue(aliceOrders.any { it.id == createdOrder.id })

        val adminOrders = client.get("/orders") { bearerAuth(adminToken) }.body<List<OrderResponse>>()
        assertTrue(adminOrders.any { it.id == createdOrder.id })
    }

    @Test
    fun `logout blacklists the token`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }
        val client = jsonClient()
        val token = client.login("alice", "alice123")

        val logoutResponse = client.post("/auth/logout") { bearerAuth(token) }
        assertEquals(HttpStatusCode.NoContent, logoutResponse.status)

        val reused = client.get("/pizzas") { bearerAuth(token) }
        assertEquals(HttpStatusCode.Unauthorized, reused.status)
    }

    @Test
    fun `repeated failed logins are rate limited`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }
        val client = jsonClient()

        val statuses = (1..10).map {
            client.post("/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest("alice", "wrong-password"))
            }.status
        }
        assertTrue(statuses.contains(HttpStatusCode.TooManyRequests))
    }
}
