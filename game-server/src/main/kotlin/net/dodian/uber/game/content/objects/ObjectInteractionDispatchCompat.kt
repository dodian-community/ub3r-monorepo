package net.dodian.uber.game.content.objects

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.services.ObjectInteractionContext
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.interaction.DispatchTiming

/**
 * Compatibility wrapper. Use [ObjectInteractionService] for new code.
 */
object ObjectInteractionDispatchCompat {
    @JvmStatic
    fun tryHandleClick(
        client: Client,
        option: Int,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): Boolean = ObjectInteractionService.tryHandleClick(client, option, objectId, position, obj)

    @JvmStatic
    fun tryHandleUseItem(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
        itemId: Int,
        itemSlot: Int,
        interfaceId: Int,
    ): Boolean = ObjectInteractionService.tryHandleUseItem(client, objectId, position, obj, itemId, itemSlot, interfaceId)

    @JvmStatic
    fun tryHandleMagic(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
        spellId: Int,
    ): Boolean = ObjectInteractionService.tryHandleMagic(client, objectId, position, obj, spellId)

    @JvmStatic
    fun tryHandle(context: ObjectInteractionContext): Boolean = ObjectInteractionService.tryHandle(context)

    @JvmStatic
    fun tryHandleTimed(context: ObjectInteractionContext): DispatchTiming = ObjectInteractionService.tryHandleTimed(context)
}
