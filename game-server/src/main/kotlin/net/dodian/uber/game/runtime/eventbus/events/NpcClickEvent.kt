package net.dodian.uber.game.runtime.eventbus.events

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.eventbus.GameEvent

data class NpcClickEvent(
    val client: Client,
    val option: Int,
    val npc: Npc,
) : GameEvent
