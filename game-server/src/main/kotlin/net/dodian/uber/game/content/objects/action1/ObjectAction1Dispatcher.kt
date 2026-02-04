package net.dodian.uber.game.content.objects.action1

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import org.slf4j.LoggerFactory

object ObjectAction1Dispatcher {
    private val logger = LoggerFactory.getLogger(ObjectAction1Dispatcher::class.java)

    @JvmStatic
    fun tryHandle(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        val content = ObjectAction1Registry.get(objectId) ?: return false
        return try {
            content.onClick1(client, objectId, position, obj)
        } catch (e: Exception) {
            logger.error("Error handling object click1 (objectId={}) via {}", objectId, content::class.java.name, e)
            false
        }
    }
}

