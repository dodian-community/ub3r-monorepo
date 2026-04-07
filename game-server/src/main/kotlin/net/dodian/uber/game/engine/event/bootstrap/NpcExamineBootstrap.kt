package net.dodian.uber.game.engine.event.bootstrap

import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.npc.NpcExamineEvent

/** Handles examine-NPC responses wired from NpcExamineEvent. */
object NpcExamineBootstrap {
    @JvmStatic
    fun bootstrap() {
        GameEventBus.on<NpcExamineEvent> { event ->
            event.client.examineNpc(event.client, event.npcId)
            true
        }
    }
}

