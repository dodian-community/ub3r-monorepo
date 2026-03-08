package net.dodian.uber.game.content.objects

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

/**
 * Backward-compatible wrapper. Use [ObjectInteractionService] for new code.
 */
object ObjectInteractionAdapterCompat {
    @JvmStatic
    fun tryHandle(
        client: Client,
        option: Int,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): Boolean {
        return ObjectInteractionService.tryHandleClick(client, option, objectId, position, obj)
    }

    @JvmStatic
    fun tryHandleUseItem(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
        itemId: Int,
        itemSlot: Int,
        interfaceId: Int,
    ): Boolean {
        return ObjectInteractionService.tryHandleUseItem(client, objectId, position, obj, itemId, itemSlot, interfaceId)
    }

    @JvmStatic
    fun tryHandleMagic(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
        spellId: Int,
    ): Boolean {
        return ObjectInteractionService.tryHandleMagic(client, objectId, position, obj, spellId)
    }
}
