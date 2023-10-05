package net.dodian.uber.net.protocol.handlers

import com.github.michaelbull.logging.InlineLogger
import net.dodian.uber.game.model.player.packets.incoming.ClickingButtons
import net.dodian.uber.game.modelkt.World
import net.dodian.uber.game.modelkt.entity.player.Player
import net.dodian.uber.net.protocol.packets.client.ButtonMessage
import net.dodian.uber.net.protocol.packets.server.LogoutMessage

private val logger = InlineLogger()

class ButtonMessageHandler(world: World) : MessageHandler<ButtonMessage>(world) {

    override fun handle(player: Player, message: ButtonMessage) = with(message.widgetId) {

        when (this) {
            2458 -> player.send(LogoutMessage())
            1164 -> player.sendMessage("Ok")
            else -> {
                logger.debug { "${player.username} clicked unhandled button '$this', falling back to old ClickingButtons." }
                ClickingButtons.handleButton(player.oldClient, this)
            }
        }
    }
}