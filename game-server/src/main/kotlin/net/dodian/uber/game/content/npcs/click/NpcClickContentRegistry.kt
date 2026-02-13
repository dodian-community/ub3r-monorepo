package net.dodian.uber.game.content.npcs.click

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object NpcClickContentRegistry {
    private val logger = LoggerFactory.getLogger(NpcClickContentRegistry::class.java)

    private val loaded = AtomicBoolean(false)
    private val byNpcId = ConcurrentHashMap<Int, NpcClickContent>()

    fun ensureLoaded() {
        if (!loaded.compareAndSet(false, true)) return

        register(net.dodian.uber.game.content.npcs.click.impl.BankerNpcClicks)
        register(net.dodian.uber.game.content.npcs.click.impl.Monk555Clicks)
    }

    fun register(content: NpcClickContent) {
        for (npcId in content.npcIds) {
            val existing = byNpcId.putIfAbsent(npcId, content)
            if (existing != null) {
                logger.error(
                    "Duplicate NpcClickContent for npcId={} (existing={}, new={})",
                    npcId,
                    existing::class.java.name,
                    content::class.java.name
                )
            }
        }
    }

    fun get(npcId: Int): NpcClickContent? {
        ensureLoaded()
        return byNpcId[npcId]
    }
}

