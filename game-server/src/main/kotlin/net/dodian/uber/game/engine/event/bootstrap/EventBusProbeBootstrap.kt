package net.dodian.uber.game.engine.event.bootstrap

import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.widget.CommandEvent
import net.dodian.uber.game.netty.listener.out.SendMessage

object EventBusProbeBootstrap {
    @JvmStatic
    fun bootstrap() {
        GameEventBus.on<CommandEvent>(
            condition = { it.rawCommand.equals("eventbus_probe", ignoreCase = true) },
        ) { event ->
            event.client.sendMessage("Event bus probe handled.")
            true
        }
    }
}
