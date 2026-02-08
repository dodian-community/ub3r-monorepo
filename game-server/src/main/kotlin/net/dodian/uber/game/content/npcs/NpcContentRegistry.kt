package net.dodian.uber.game.content.npcs

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object NpcContentRegistry {
    private val logger = LoggerFactory.getLogger(NpcContentRegistry::class.java)

    private val loaded = AtomicBoolean(false)
    private val byNpcId = ConcurrentHashMap<Int, NpcContent>()
    private val contents = mutableListOf<NpcContent>()

    fun ensureLoaded() {
        if (!loaded.compareAndSet(false, true)) return
        register(net.dodian.uber.game.content.npcs.yanille.Rat)
        register(net.dodian.uber.game.content.npcs.yanille.Cow)
        register(net.dodian.uber.game.content.npcs.yanille.BattleMage)
        register(net.dodian.uber.game.content.npcs.yanille.AbyssalGuardian)
        register(net.dodian.uber.game.content.npcs.yanille.MakeoverMage)
        register(net.dodian.uber.game.content.npcs.yanille.GuardNpc)
        register(net.dodian.uber.game.content.npcs.yanille.Monk)
        register(net.dodian.uber.game.content.npcs.yanille.Shopkeepers)
        register(net.dodian.uber.game.content.npcs.yanille.Farmers)
    }

    fun register(content: NpcContent) {
        contents.add(content)
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

    @JvmStatic
    fun getSpawnDefinitions(): List<NpcSpawnDef> {
        ensureLoaded()
        return contents.flatMap { content ->
            val presets = content.npcDataPresets()
            content.spawns().map { spawn ->
                val preset = if (spawn.npcDataSlot in presets.indices) {
                    presets[spawn.npcDataSlot]
                } else {
                    if (spawn.npcDataSlot >= 0) {
                        logger.error(
                            "Missing NpcDataPreset slot={} for content={} npcId={}",
                            spawn.npcDataSlot,
                            content::class.java.name,
                            spawn.npcId
                        )
                    }
                    null
                }
                if (preset == null) {
                    spawn
                } else {
                    spawn.copy(
                        respawnTicks = if (spawn.respawnTicks != -1) spawn.respawnTicks else preset.respawnTicks,
                        attack = if (spawn.attack != -1) spawn.attack else preset.attack,
                        defence = if (spawn.defence != -1) spawn.defence else preset.defence,
                        strength = if (spawn.strength != -1) spawn.strength else preset.strength,
                        hitpoints = if (spawn.hitpoints != -1) spawn.hitpoints else preset.hitpoints,
                        ranged = if (spawn.ranged != -1) spawn.ranged else preset.ranged,
                        magic = if (spawn.magic != -1) spawn.magic else preset.magic
                    )
                }
            }
        }
    }
}
