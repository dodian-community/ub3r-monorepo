package net.dodian.uber.game.engine.systems.net

import net.dodian.cache.objects.GameObjectData
import net.dodian.uber.game.engine.util.Misc
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.interaction.ItemOnObjectIntent
import net.dodian.uber.game.engine.systems.interaction.ObjectClickIntent
import net.dodian.uber.game.engine.systems.interaction.scheduler.InteractionTaskScheduler
import net.dodian.uber.game.engine.systems.interaction.scheduler.ObjectInteractionTask
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry

/**
 * Kotlin service for object-interaction packet side-effects that must stay out
 * of Netty inbound listeners.
 */
object PacketObjectService {
    /**
     * Processes an object-click packet after the listener has decoded the click
     * option and object coordinates/id.
     */
    @JvmStatic
    fun handleObjectClick(
        client: Client,
        opcode: Int,
        option: Int,
        objectId: Int,
        objectX: Int,
        objectY: Int,
    ) {
        if (client.randomed || client.UsingAgility) return

        val targetPosition = Position(objectX, objectY, client.position.z)
        val objectDef = Misc.getObject(objectId, objectX, objectY, client.position.z)
        val objectData = GameObjectData.forId(objectId)
        val intent = ObjectClickIntent(
            opcode = opcode,
            createdCycle = PlayerRegistry.cycle.toLong(),
            option = option,
            objectId = objectId,
            objectPosition = targetPosition,
            objectData = objectData,
            objectDef = objectDef,
        )
        InteractionTaskScheduler.schedule(client, intent, ObjectInteractionTask(client, intent))
    }

    /**
     * Processes an item-on-object packet after the listener has decoded packet
     * fields.
     */
    @JvmStatic
    fun handleItemOnObject(
        client: Client,
        opcode: Int,
        interfaceId: Int,
        objectId: Int,
        objectX: Int,
        objectY: Int,
        itemSlot: Int,
        itemId: Int,
    ) {
        if (client.randomed) return
        if (itemSlot < 0 || itemSlot >= client.playerItems.size || interfaceId != 3214) return

        val targetPosition = Position(objectX, objectY, client.position.z)
        val objectData = GameObjectData.forId(objectId)
        val objectDef = Misc.getObject(objectId, targetPosition.x, targetPosition.y, client.position.z)
        val intent = ItemOnObjectIntent(
            opcode = opcode,
            createdCycle = PlayerRegistry.cycle.toLong(),
            interfaceId = interfaceId,
            itemSlot = itemSlot,
            itemId = itemId,
            objectId = objectId,
            objectPosition = targetPosition,
            objectData = objectData,
            objectDef = objectDef,
        )
        InteractionTaskScheduler.schedule(client, intent, ObjectInteractionTask(client, intent))
    }
}


