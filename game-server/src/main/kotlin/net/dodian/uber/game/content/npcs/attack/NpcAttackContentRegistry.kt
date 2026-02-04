package net.dodian.uber.game.content.npcs.attack

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object NpcAttackContentRegistry {
    private val logger = LoggerFactory.getLogger(NpcAttackContentRegistry::class.java)

    private val loaded = AtomicBoolean(false)
    private val byNpcId = ConcurrentHashMap<Int, NpcAttackContent>()

    fun ensureLoaded() {
        if (!loaded.compareAndSet(false, true)) {
            return
        }
        register(Guard388AttackNpc)
        register(Farmer3086AttackNpc)
    }

    fun register(content: NpcAttackContent) {
        for (npcId in content.npcIds) {
            val existing = byNpcId.putIfAbsent(npcId, content)
            if (existing != null) {
                logger.error(
                    "Duplicate NpcAttackContent for npcId={} (existing={}, new={})",
                    npcId,
                    existing::class.java.name,
                    content::class.java.name
                )
            }
        }
    }

    fun get(npcId: Int): NpcAttackContent? {
        ensureLoaded()
        return byNpcId[npcId]
    }
}
