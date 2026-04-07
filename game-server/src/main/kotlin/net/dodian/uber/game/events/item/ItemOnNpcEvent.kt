package net.dodian.uber.game.events.item

import net.dodian.uber.game.events.GameEvent
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

/** Fired when a player uses an inventory item on an NPC. */
data class ItemOnNpcEvent(
    val client: Client,
    val itemId: Int,
    val itemSlot: Int,
    val npcIndex: Int,
    val npc: Npc,
) : GameEvent

