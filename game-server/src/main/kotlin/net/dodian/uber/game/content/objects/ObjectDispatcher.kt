package net.dodian.uber.game.content.objects

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import org.slf4j.LoggerFactory

/**
 * Java-friendly entrypoint for routing object interactions to Kotlin content.
 */
object ObjectDispatcher {
    private val logger = LoggerFactory.getLogger(ObjectDispatcher::class.java)

    @JvmStatic
    fun tryHandle(
        client: Client,
        option: Int,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): Boolean {
        val content = ObjectContentRegistry.get(objectId) ?: return false
        return try {
            when (option) {
                1 -> content.onFirstClick(client, objectId, position, obj)
                2 -> content.onSecondClick(client, objectId, position, obj)
                3 -> content.onThirdClick(client, objectId, position, obj)
                4 -> content.onFourthClick(client, objectId, position, obj)
                5 -> content.onFifthClick(client, objectId, position, obj)
                else -> false
            }
        } catch (e: Exception) {
            logger.error(
                "Error handling object interaction (option={}, objectId={}) via {}",
                option,
                objectId,
                content::class.java.name,
                e
            )
            false
        }
    }
}

