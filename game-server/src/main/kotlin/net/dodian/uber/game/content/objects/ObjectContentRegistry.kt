package net.dodian.uber.game.content.objects

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object ObjectContentRegistry {
    private val logger = LoggerFactory.getLogger(ObjectContentRegistry::class.java)

    private val loaded = AtomicBoolean(false)
    private val byObjectId = ConcurrentHashMap<Int, ObjectContent>()

    fun ensureLoaded() {
        if (!loaded.compareAndSet(false, true)) return
        register(net.dodian.uber.game.content.objects.mining.CoalRock7489)
        register(net.dodian.uber.game.content.objects.smelting.FurnaceObject)
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

    fun get(objectId: Int): ObjectContent? {
        ensureLoaded()
        return byObjectId[objectId]
    }
}

