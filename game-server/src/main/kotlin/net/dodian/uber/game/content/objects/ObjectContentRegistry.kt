package net.dodian.uber.game.content.objects

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.content.objects.mining.CoalRock7489
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object ObjectContentRegistry {
    private val logger = LoggerFactory.getLogger(ObjectContentRegistry::class.java)

    private val loaded = AtomicBoolean(false)
    private val byObjectId = ConcurrentHashMap<Int, ObjectContent>()

    fun ensureLoaded() {
        if (!loaded.compareAndSet(false, true)) {
            return
        }
        register(FurnaceObject)
        register(CoalRock7489)
    }

    fun register(content: ObjectContent) {
        for (objectId in content.objectIds) {
            val existing = byObjectId.putIfAbsent(objectId, content)
            if (existing != null) {
                logger.error(
                    "Duplicate ObjectContent for objectId={} (existing={}, new={})",
                    objectId,
                    existing::class.java.name,
                    content::class.java.name
                )
            }
        }
    }

    fun tryHandle(
        client: Client,
        option: Int,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): Boolean {
        ensureLoaded()
        val content = byObjectId[objectId] ?: return false
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
                "Error handling object click (option={}, objectId={}) via {}",
                option,
                objectId,
                content::class.java.name,
                e
            )
            false
        }
    }
}
