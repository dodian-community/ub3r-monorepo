package net.dodian.uber.web.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.dodian.config.Environment
import net.dodian.config.serverConfig
import net.dodian.config.serverEnvironment
import net.dodian.uber.web.configuration.jwtAudience
import net.dodian.uber.web.configuration.jwtDomain
import net.dodian.uber.web.configuration.jwtSecret
import net.dodian.uber.web.models.User
import java.util.*

fun Route.authRoutes() {
    route("/auth") {
        authenticate("auth-jwt") {
            get("/hello") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respondText { "No principal found :(" }

                val username = principal.payload.getClaim("username").asString()
                val expiresAt = principal.expiresAt?.time?.minus(System.currentTimeMillis())

                call.respondText { "Hello, $username! Token is expired at $expiresAt ms." }
            }
        }

        if (serverConfig.debugMode && serverEnvironment == Environment.Development) {
            post("/login") {
                val user = call.receive<User>()

                val token = JWT.create()
                    .withAudience(jwtAudience)
                    .withIssuer(jwtDomain)
                    .withClaim("username", user.username)
                    .withExpiresAt(Date(System.currentTimeMillis() + 60_000))
                    .sign(Algorithm.HMAC256(jwtSecret))

                call.respond(hashMapOf("token" to token))
            }
        }
    }
}