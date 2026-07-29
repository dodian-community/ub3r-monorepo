package net.dodian.uber.game.ui

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage
import org.slf4j.LoggerFactory

/** Sends a client URL-opening message without coupling UI callers to Player internals. */
object ExternalUrlService {
    private val logger = LoggerFactory.getLogger(ExternalUrlService::class.java)

    @JvmStatic
    fun open(client: Client, url: String) {
        try {
            client.send(SendMessage("$url#url#"))
        } catch (exception: Exception) {
            logger.warn("Unable to open external page for {}", client.playerName, exception)
        }
    }
}
