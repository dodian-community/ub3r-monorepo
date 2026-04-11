package net.dodian.uber.game.engine.webapi

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import spark.Spark.get
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean

data class ServerStatus(
    var playersOnline: Set<OnlinePlayer> = emptySet(),
    val launchedAt: LocalDateTime = LocalDateTime.now(),
)

data class OnlinePlayer(
    val id: Int,
    val username: String
)

private fun getOnlinePlayers() = PlayerRegistry.playersOnline.map { (_, player) ->
    OnlinePlayer(player.dbId, player.playerName)
}

private val mapper: ObjectMapper = ObjectMapper()
    .findAndRegisterModules()
    .registerKotlinModule()
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .enable(SerializationFeature.INDENT_OUTPUT)

object WebApi {
    private val started = AtomicBoolean(false)
    private val serverStatus = ServerStatus()

    @JvmStatic
    fun start() {
        if (!started.compareAndSet(false, true)) {
            return
        }

        get("/api/server-status") { _, res ->
            serverStatus.playersOnline = getOnlinePlayers().toSet()

            res.header("Access-Control-Allow-Origin", "*")
            res.header("Access-Control-Allow-Credentials", "true")
            res.header("Access-Control-Allow-Methods", "*")
            res.header("Access-Control-Allow-Headers", "*")
            res.type("application/json")

            return@get mapper.writeValueAsString(serverStatus)
        }
    }
}

@Deprecated("Use WebApi.start() from engine bootstrap")
fun launchWebApi() = WebApi.start()
