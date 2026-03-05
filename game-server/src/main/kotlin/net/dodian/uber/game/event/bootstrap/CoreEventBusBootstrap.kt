package net.dodian.uber.game.event.bootstrap

import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.event.GameEventBus
import net.dodian.uber.game.event.events.CommandEvent

object CoreEventBusBootstrap {
    @JvmStatic
    fun bootstrap() {
        GameEventBus.on<CommandEvent>(
            condition = { it.rawCommand.equals("eventbus_probe", ignoreCase = true) },
        ) { event ->
            event.client.send(SendMessage("Event bus probe handled."))
            true
        }
    }
}
