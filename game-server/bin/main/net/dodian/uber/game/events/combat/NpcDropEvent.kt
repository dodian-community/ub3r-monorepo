package net.dodian.uber.game.events.combat

import net.dodian.uber.game.events.GameEvent
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.GameItem
import net.dodian.uber.game.model.Position

/**
 * Fired after NPC loot is rolled and placed on the ground.
 * Use this to hook into drop announcements, logging, or custom loot modification.
 */
data class NpcDropEvent(
    val npc: Npc,
    val killer: Client,
    val drops: List<GameItem>,
    val position: Position,
) : GameEvent

