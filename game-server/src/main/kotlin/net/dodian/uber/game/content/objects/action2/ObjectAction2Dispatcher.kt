package net.dodian.uber.game.content.objects.action2

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import org.slf4j.LoggerFactory

object ObjectAction2Dispatcher {
    private val logger = LoggerFactory.getLogger(ObjectAction2Dispatcher::class.java)

    @JvmStatic
    fun tryHandle(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        val content = ObjectAction2Registry.get(objectId) ?: return false
        return try {
            content.onClick2(client, objectId, position, obj)
        } catch (e: Exception) {
            logger.error("Error handling object click2 (objectId={}) via {}", objectId, content::class.java.name, e)
            false
        }
    }
}

