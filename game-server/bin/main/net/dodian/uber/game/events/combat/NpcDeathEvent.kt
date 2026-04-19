package net.dodian.uber.game.events.combat

import net.dodian.uber.game.events.GameEvent
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

/**
 * Fired when an NPC dies.
 * [killer] is the player who dealt the killing blow, or null for environment kills.
 */
data class NpcDeathEvent(
    val npc: Npc,
    val killer: Client?,
    val cycle: Long,
) : GameEvent

