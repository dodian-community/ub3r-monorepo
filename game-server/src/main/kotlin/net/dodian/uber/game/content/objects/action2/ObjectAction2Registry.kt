package net.dodian.uber.game.content.objects.action2

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object ObjectAction2Registry {
    private val logger = LoggerFactory.getLogger(ObjectAction2Registry::class.java)

    private val loaded = AtomicBoolean(false)
    private val byObjectId = ConcurrentHashMap<Int, ObjectAction2Content>()

    fun ensureLoaded() {
        if (!loaded.compareAndSet(false, true)) return
        // registrations added below
        register(net.dodian.uber.game.content.objects.action2.mining.CoalRock7489Prospect)
        register(net.dodian.uber.game.content.objects.action2.smelting.FurnaceGoldClick)
    }

    fun register(content: ObjectAction2Content) {
        for (objectId in content.objectIds) {
            val existing = byObjectId.putIfAbsent(objectId, content)
            if (existing != null) {
                logger.error(
                    "Duplicate ObjectAction2Content for objectId={} (existing={}, new={})",
                    objectId,
                    existing::class.java.name,
                    content::class.java.name
                )
            }
        }
    }

    fun get(objectId: Int): ObjectAction2Content? {
        ensureLoaded()
        return byObjectId[objectId]
    }
}

