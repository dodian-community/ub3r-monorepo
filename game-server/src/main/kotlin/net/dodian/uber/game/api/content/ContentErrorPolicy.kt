package net.dodian.uber.game.api.content

import net.dodian.uber.game.model.entity.player.Client
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object ContentErrorPolicy {
    private val logger: Logger = LoggerFactory.getLogger(ContentErrorPolicy::class.java)

    @JvmStatic
    fun runBoolean(
        player: Client,
        scope: String,
        defaultValue: Boolean = false,
        action: () -> Boolean,
    ): Boolean {
        return try {
            action()
        } catch (throwable: Throwable) {
            logger.error(
                "Content handler failure scope={} slot={} name={} pos={}",
                scope,
                player.slot,
                player.playerName,
                player.position,
                throwable,
            )
            defaultValue
        }
    }

    @JvmStatic
    fun <T> runNullable(
        player: Client,
        scope: String,
        action: () -> T?,
    ): T? {
        return try {
            action()
        } catch (throwable: Throwable) {
            logger.error(
                "Content handler failure scope={} slot={} name={} pos={}",
                scope,
                player.slot,
                player.playerName,
                player.position,
                throwable,
            )
            null
        }
    }
}
