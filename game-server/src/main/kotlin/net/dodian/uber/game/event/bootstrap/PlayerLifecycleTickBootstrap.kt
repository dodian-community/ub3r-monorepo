package net.dodian.uber.game.event.bootstrap

import net.dodian.uber.game.event.GameEventBus
import net.dodian.uber.game.event.events.PlayerTickEvent
import net.dodian.uber.game.engine.lifecycle.PlayerLifecycleTickService

object PlayerLifecycleTickBootstrap {
    @JvmStatic
    fun bootstrap() {
        GameEventBus.on<PlayerTickEvent>(
            action = { event ->
                PlayerLifecycleTickService.processBeforeCombat(event.player)
                true
            },
        )
        GameEventBus.on<PlayerTickEvent>(
            action = { event ->
                PlayerLifecycleTickService.processAfterCombat(event.player, event.wallClockNow)
                true
            },
        )
        GameEventBus.on<PlayerTickEvent>(
            action = { event ->
                PlayerLifecycleTickService.processEffectsPeriodicPersistence(event.player, event.wallClockNow)
                true
            },
        )
    }
}
