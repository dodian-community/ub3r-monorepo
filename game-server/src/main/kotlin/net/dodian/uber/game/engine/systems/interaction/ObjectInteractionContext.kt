package net.dodian.uber.game.engine.systems.interaction

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

enum class ObjectInteractionType {
    CLICK,
    USE_ITEM,
    MAGIC,
}

data class ObjectInteractionContext(
    val type: ObjectInteractionType,
    val client: Client,
    val packetOpcode: Int? = null,
    val objectId: Int,
    val position: Position,
    val obj: GameObjectData?,
    val option: Int? = null,
    val itemId: Int? = null,
    val itemSlot: Int? = null,
    val interfaceId: Int? = null,
    val spellId: Int? = null,
) {
    companion object {
        fun click(
            client: Client,
            option: Int,
            objectId: Int,
            position: Position,
            obj: GameObjectData?,
            packetOpcode: Int? = null,
        ): ObjectInteractionContext {
            return ObjectInteractionContext(
                type = ObjectInteractionType.CLICK,
                client = client,
                packetOpcode = packetOpcode,
                objectId = objectId,
                position = position,
                obj = obj,
                option = option,
            )
        }

        fun useItem(
            client: Client,
            objectId: Int,
            position: Position,
            obj: GameObjectData?,
            itemId: Int,
            itemSlot: Int,
            interfaceId: Int,
            packetOpcode: Int? = null,
        ): ObjectInteractionContext {
            return ObjectInteractionContext(
                type = ObjectInteractionType.USE_ITEM,
                client = client,
                packetOpcode = packetOpcode,
                objectId = objectId,
                position = position,
                obj = obj,
                itemId = itemId,
                itemSlot = itemSlot,
                interfaceId = interfaceId,
            )
        }

        fun magic(
            client: Client,
            objectId: Int,
            position: Position,
            obj: GameObjectData?,
            spellId: Int,
            packetOpcode: Int? = null,
        ): ObjectInteractionContext {
            return ObjectInteractionContext(
                type = ObjectInteractionType.MAGIC,
                client = client,
                packetOpcode = packetOpcode,
                objectId = objectId,
                position = position,
                obj = obj,
                spellId = spellId,
            )
        }
    }
}
