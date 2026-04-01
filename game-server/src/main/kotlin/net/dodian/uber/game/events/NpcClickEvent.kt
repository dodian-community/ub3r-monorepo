package net.dodian.uber.game.events

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.event.GameEvent

data class NpcClickEvent(
    val client: Client,
    val option: Int,
    val npc: Npc,
) : GameEvent
