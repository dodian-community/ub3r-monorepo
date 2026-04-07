package net.dodian.uber.game.engine.event.bootstrap

import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.item.ItemDropEvent
import net.dodian.uber.game.events.item.ItemExamineEvent
import net.dodian.uber.game.events.npc.NpcExamineEvent
import net.dodian.uber.game.events.objects.ObjectExamineEvent

/**
 * Bridges packet-origin gameplay events to legacy handlers so events are always wired.
 */
@Suppress("unused")
object PacketGameplayEventWiring {
    @JvmStatic
    fun bootstrap() {
        GameEventBus.on<ItemDropEvent> { event ->
            event.client.dropItem(event.itemId, event.slot)
            true
        }

        GameEventBus.on<ItemExamineEvent> { event ->
            event.client.examineItem(event.client, event.itemId, event.contextValue)
            true
        }

        GameEventBus.on<NpcExamineEvent> { event ->
            event.client.examineNpc(event.client, event.npcId)
            true
        }

        GameEventBus.on<ObjectExamineEvent> { event ->
            event.client.examineObject(event.client, event.objectId, event.position)
            true
        }
    }
}
