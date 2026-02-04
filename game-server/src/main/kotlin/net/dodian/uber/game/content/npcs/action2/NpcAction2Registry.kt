package net.dodian.uber.game.content.npcs.action2

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object NpcAction2Registry {
    private val logger = LoggerFactory.getLogger(NpcAction2Registry::class.java)

    private val loaded = AtomicBoolean(false)
    private val byNpcId = ConcurrentHashMap<Int, NpcAction2Content>()

    fun ensureLoaded() {
        if (!loaded.compareAndSet(false, true)) return
        register(Farmer3086)
    }

    fun register(content: NpcAction2Content) {
        for (npcId in content.npcIds) {
            val existing = byNpcId.putIfAbsent(npcId, content)
            if (existing != null) {
                logger.error(
                    "Duplicate NpcAction2Content for npcId={} (existing={}, new={})",
                    npcId,
                    existing::class.java.name,
                    content::class.java.name
                )
            }
        }
    }

    fun get(npcId: Int): NpcAction2Content? {
        ensureLoaded()
        return byNpcId[npcId]
    }
}

