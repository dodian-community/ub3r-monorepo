package net.dodian.uber.game.event.events

import net.dodian.uber.game.event.GameEvent
import net.dodian.uber.game.model.entity.player.Client

data class PlayerTickEvent(
    val player: Client,
    val cycle: Long,
    val wallClockNow: Long,
) : GameEvent
