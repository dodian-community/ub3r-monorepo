package net.dodian.uber.web.configuration

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*

fun Application.module() {
    configureAuthentication()
    configureSerialization()
    configureRoutes()
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        jackson {
            findAndRegisterModules()
            registerKotlinModule()
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }
}
