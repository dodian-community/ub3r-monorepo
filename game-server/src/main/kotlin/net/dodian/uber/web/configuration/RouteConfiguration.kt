package net.dodian.uber.web.configuration

import io.ktor.server.application.*
import io.ktor.server.routing.*
import net.dodian.uber.web.routes.authRoutes
import net.dodian.uber.web.routes.gameRoutes

fun Application.configureRoutes() {
    routing {
        route("/api/v1") {
            authRoutes()
            gameRoutes()
        }
    }
}