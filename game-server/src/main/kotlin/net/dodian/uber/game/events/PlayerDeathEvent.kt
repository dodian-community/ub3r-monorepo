package net.dodian.uber.game.events

import net.dodian.uber.game.model.entity.player.Client

/** Fired when a player dies. */
data class PlayerDeathEvent(
    val player: Client,
    val cycle: Long,
) : GameEvent
