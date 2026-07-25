package net.dodian.uber.game.persistence.account.discord

import com.fasterxml.jackson.databind.ObjectMapper
import net.dodian.uber.game.engine.config.discordApiEnabled
import net.dodian.uber.game.engine.config.discordClientId
import net.dodian.uber.game.engine.config.discordClientSecret
import net.dodian.uber.game.engine.config.discordRedirectUrl
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

data class DiscordUser(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val verified: Boolean = false
)

object DiscordApiService {
    private val logger = LoggerFactory.getLogger(DiscordApiService::class.java)
    private val httpClient = HttpClient.newHttpClient()
    private val objectMapper = ObjectMapper()

    @JvmStatic
    fun fetchDiscordUser(code: String): DiscordUser? {
        if (code.isBlank()) return null
        try {
            if (!discordApiEnabled || discordClientId.isEmpty()) return null

            val params = mapOf(
                "client_id" to discordClientId,
                "client_secret" to discordClientSecret,
                "grant_type" to "authorization_code",
                "code" to code,
                "redirect_uri" to discordRedirectUrl
            )

            val formBody = params.map { (k, v) ->
                "${URLEncoder.encode(k, StandardCharsets.UTF_8)}=${URLEncoder.encode(v, StandardCharsets.UTF_8)}"
            }.joinToString("&")

            val tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://discord.com/api/oauth2/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build()

            val tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString())
            if (tokenResponse.statusCode() != 200) {
                logger.error("Discord token exchange failed: status={} body={}", tokenResponse.statusCode(), tokenResponse.body())
                return null
            }

            val tokenJson = objectMapper.readTree(tokenResponse.body())
            val accessToken = tokenJson.get("access_token")?.asText() ?: return null

            val userRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://discord.com/api/v10/users/@me"))
                .header("Authorization", "Bearer $accessToken")
                .GET()
                .build()

            val userResponse = httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString())
            if (userResponse.statusCode() != 200) {
                logger.error("Discord user fetch failed: status={} body={}", userResponse.statusCode(), userResponse.body())
                return null
            }

            val userJson = objectMapper.readTree(userResponse.body())
            val discordUser = DiscordUser(
                id = userJson.get("id")?.asText() ?: "",
                username = userJson.get("username")?.asText() ?: "",
                email = userJson.get("email")?.asText() ?: "",
                verified = userJson.get("verified")?.asBoolean() ?: false
            )
            logger.info("Successfully fetched Discord user for account creation: id={} username={} email={}", discordUser.id, discordUser.username, discordUser.email)
            return discordUser
        } catch (e: Exception) {
            logger.error("Error fetching Discord user for code {}", code, e)
            return null
        }
    }
}
