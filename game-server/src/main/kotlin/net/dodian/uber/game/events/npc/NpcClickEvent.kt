package net.dodian.uber.game.events.npc

import net.dodian.uber.game.events.GameEvent
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

/** Fired when a player clicks an option on an NPC. [option] is 1-based (1 = first menu option). */
data class NpcClickEvent(
    val client: Client,
    val option: Int,
    val npc: Npc,
) : GameEvent
