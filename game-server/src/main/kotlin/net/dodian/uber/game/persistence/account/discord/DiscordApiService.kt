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
import java.time.Duration

data class DiscordUser(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val verified: Boolean = false
)

sealed interface DiscordFetchResult {
    data class Success(val user: DiscordUser) : DiscordFetchResult
    data object InvalidAuthorization : DiscordFetchResult
    data object Unavailable : DiscordFetchResult
}

object DiscordApiService {
    private val logger = LoggerFactory.getLogger(DiscordApiService::class.java)
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()
    private val objectMapper = ObjectMapper()

    @JvmStatic
    fun fetchDiscordUser(code: String): DiscordUser? =
        (fetchDiscordUserResult(code) as? DiscordFetchResult.Success)?.user

    @JvmStatic
    fun fetchDiscordUserResult(code: String): DiscordFetchResult {
        if (code.isBlank()) return DiscordFetchResult.InvalidAuthorization
        try {
            if (!discordApiEnabled || discordClientId.isEmpty() || discordClientSecret.isEmpty()) {
                return DiscordFetchResult.Unavailable
            }

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
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build()

            val tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString())
            if (tokenResponse.statusCode() != 200) {
                if (tokenResponse.statusCode() in 400..401) {
                    logger.debug("Discord token exchange rejected: status={}", tokenResponse.statusCode())
                } else {
                    logger.warn("Discord token exchange failed: status={}", tokenResponse.statusCode())
                }
                return if (tokenResponse.statusCode() in 400..401) {
                    DiscordFetchResult.InvalidAuthorization
                } else {
                    DiscordFetchResult.Unavailable
                }
            }

            val tokenJson = objectMapper.readTree(tokenResponse.body())
            val accessToken = tokenJson.get("access_token")?.asText()
                ?: return DiscordFetchResult.InvalidAuthorization

            val userRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://discord.com/api/v10/users/@me"))
                .header("Authorization", "Bearer $accessToken")
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build()

            val userResponse = httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString())
            if (userResponse.statusCode() != 200) {
                logger.warn("Discord user fetch failed: status={}", userResponse.statusCode())
                return if (userResponse.statusCode() in 400..401) {
                    DiscordFetchResult.InvalidAuthorization
                } else {
                    DiscordFetchResult.Unavailable
                }
            }

            val userJson = objectMapper.readTree(userResponse.body())
            val discordUser = DiscordUser(
                id = userJson.get("id")?.asText() ?: "",
                username = userJson.get("username")?.asText() ?: "",
                email = userJson.get("email")?.asText() ?: "",
                verified = userJson.get("verified")?.asBoolean() ?: false
            )
            logger.info("Successfully verified Discord identity for account creation id={}", discordUser.id)
            return DiscordFetchResult.Success(discordUser)
        } catch (e: Exception) {
            logger.error("Discord account-creation authorization failed", e)
            return DiscordFetchResult.Unavailable
        }
    }
}
