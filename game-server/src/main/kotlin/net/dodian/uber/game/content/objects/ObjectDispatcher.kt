package net.dodian.uber.game.content.objects

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

/**
 * Backward-compatible wrapper. Use ObjectContentDispatcher for new code.
 */
object ObjectDispatcher {
    @JvmStatic
    fun tryHandle(
        client: Client,
        option: Int,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): Boolean {
        return ObjectContentDispatcher.tryHandleClick(client, option, objectId, position, obj)
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
        return ObjectContentDispatcher.tryHandleUseItem(client, objectId, position, obj, itemId, itemSlot, interfaceId)
    }

    @JvmStatic
    fun tryHandleMagic(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
        spellId: Int,
    ): Boolean {
        return ObjectContentDispatcher.tryHandleMagic(client, objectId, position, obj, spellId)
    }
}
