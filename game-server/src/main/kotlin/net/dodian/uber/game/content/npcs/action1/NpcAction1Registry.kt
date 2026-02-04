package net.dodian.uber.game.content.npcs.action1

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object NpcAction1Registry {
    private val logger = LoggerFactory.getLogger(NpcAction1Registry::class.java)

    private val loaded = AtomicBoolean(false)
    private val byNpcId = ConcurrentHashMap<Int, NpcAction1Content>()

    fun ensureLoaded() {
        if (!loaded.compareAndSet(false, true)) return
        register(Shopkeeper506Trade)
    }

    fun register(content: NpcAction1Content) {
        for (npcId in content.npcIds) {
            val existing = byNpcId.putIfAbsent(npcId, content)
            if (existing != null) {
                logger.error(
                    "Duplicate NpcAction1Content for npcId={} (existing={}, new={})",
                    npcId,
                    existing::class.java.name,
                    content::class.java.name
                )
            }
        }
    }

    fun get(npcId: Int): NpcAction1Content? {
        ensureLoaded()
        return byNpcId[npcId]
    }
}

