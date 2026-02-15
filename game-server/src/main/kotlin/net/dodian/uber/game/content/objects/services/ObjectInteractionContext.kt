package net.dodian.uber.game.content.objects.services

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
        ): ObjectInteractionContext {
            return ObjectInteractionContext(
                type = ObjectInteractionType.CLICK,
                client = client,
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
        ): ObjectInteractionContext {
            return ObjectInteractionContext(
                type = ObjectInteractionType.USE_ITEM,
                client = client,
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
        ): ObjectInteractionContext {
            return ObjectInteractionContext(
                type = ObjectInteractionType.MAGIC,
                client = client,
                objectId = objectId,
                position = position,
                obj = obj,
                spellId = spellId,
            )
        }
    }
}
