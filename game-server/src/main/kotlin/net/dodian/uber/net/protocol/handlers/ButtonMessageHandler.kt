package net.dodian.uber.net.protocol.handlers

import com.github.michaelbull.logging.InlineLogger
import net.dodian.uber.game.modelkt.World
import net.dodian.uber.game.modelkt.entity.player.Player
import net.dodian.uber.net.protocol.packets.client.ButtonMessage
import net.dodian.uber.net.protocol.packets.server.LogoutMessage

private val logger = InlineLogger()

class ButtonMessageHandler(world: World) : MessageHandler<ButtonMessage>(world) {

    override fun handle(player: Player, message: ButtonMessage) {
        logger.info { "${player.username} clicked button: $message" }

        if (message.widgetId == 2458)
            player.send(LogoutMessage())
    }
}