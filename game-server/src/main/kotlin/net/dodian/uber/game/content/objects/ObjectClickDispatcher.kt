package net.dodian.uber.game.content.objects

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

/**
 * Java-friendly entrypoint for routing object clicks to Kotlin content.
 */
object ObjectClickDispatcher {
    @JvmStatic
    fun tryHandle(
        client: Client,
        option: Int,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): Boolean {
        return ObjectContentRegistry.tryHandle(client, option, objectId, position, obj)
    }
}
