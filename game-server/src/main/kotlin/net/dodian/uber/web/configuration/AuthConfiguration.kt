package net.dodian.uber.web.configuration

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import net.dodian.uber.web.models.ApiError

const val jwtAudience = "jwt-audience"
const val jwtDomain = "http://localhost:8080/"
const val jwtRealm = "Access to 'hello'"
const val jwtSecret = "my-super-secret-secret"

fun Application.configureAuthentication() {
    // Please read the jwt property from the config file if you are using EngineMain
    authentication {
        jwt("auth-jwt") {
            realm = jwtRealm

            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtDomain)
                    .build()
            )

            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience))
                    JWTPrincipal(credential.payload)
                else null
            }

            challenge { defaultScheme, realm ->
                call.respond(
                    HttpStatusCode.Unauthorized, ApiError(
                        "Expired or invalid token...",
                        defaultScheme,
                        realm
                    )
                )
            }
        }
    }
}