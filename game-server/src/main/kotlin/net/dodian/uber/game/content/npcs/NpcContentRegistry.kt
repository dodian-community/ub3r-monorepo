package net.dodian.uber.game.content.npcs

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object NpcContentRegistry {
    private val logger = LoggerFactory.getLogger(NpcContentRegistry::class.java)

    private val loaded = AtomicBoolean(false)
    private val byNpcId = ConcurrentHashMap<Int, NpcContent>()

    fun ensureLoaded() {
        if (!loaded.compareAndSet(false, true)) return
        register(net.dodian.uber.game.content.npcs.guards.Guards)
        register(net.dodian.uber.game.content.npcs.shops.Shopkeepers)
        register(net.dodian.uber.game.content.npcs.thieving.Farmers)
    }

    fun register(content: NpcContent) {
        for (npcId in content.npcIds) {
            val existing = byNpcId.putIfAbsent(npcId, content)
            if (existing != null) {
                logger.error(
                    "Duplicate NpcContent for npcId={} (existing={}, new={})",
                    npcId,
                    existing::class.java.name,
                    content::class.java.name
                )
            }
        }
    }

    fun get(npcId: Int): NpcContent? {
        ensureLoaded()
        return byNpcId[npcId]
    }
}

