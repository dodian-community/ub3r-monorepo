package net.dodian.uber.game.content.objects.action1

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object ObjectAction1Registry {
    private val logger = LoggerFactory.getLogger(ObjectAction1Registry::class.java)

    private val loaded = AtomicBoolean(false)
    private val byObjectId = ConcurrentHashMap<Int, ObjectAction1Content>()

    fun ensureLoaded() {
        if (!loaded.compareAndSet(false, true)) return
        // registrations added below
        register(net.dodian.uber.game.content.objects.action1.mining.CoalRock7489)
        register(net.dodian.uber.game.content.objects.action1.smelting.FurnaceSmelt)
    }

    fun register(content: ObjectAction1Content) {
        for (objectId in content.objectIds) {
            val existing = byObjectId.putIfAbsent(objectId, content)
            if (existing != null) {
                logger.error(
                    "Duplicate ObjectAction1Content for objectId={} (existing={}, new={})",
                    objectId,
                    existing::class.java.name,
                    content::class.java.name
                )
            }
        }
    }

    fun get(objectId: Int): ObjectAction1Content? {
        ensureLoaded()
        return byObjectId[objectId]
    }
}

