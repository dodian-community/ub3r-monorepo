package net.dodian.uber.game.runtime.eventbus.bootstrap

import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.runtime.eventbus.GameEventBus
import net.dodian.uber.game.runtime.eventbus.events.CommandEvent

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
