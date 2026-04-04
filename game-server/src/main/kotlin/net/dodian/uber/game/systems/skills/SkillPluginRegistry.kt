package net.dodian.uber.game.systems.skills

import net.dodian.uber.game.systems.content.ContentBootstrap
import net.dodian.uber.game.systems.content.ContentModuleIndex
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

object SkillPluginRegistry : ContentBootstrap {
    override val id: String = "skills.registry"
    private val logger = LoggerFactory.getLogger(SkillPluginRegistry::class.java)

    private val bootstrapped = AtomicBoolean(false)
    private val definitions = mutableListOf<SkillPlugin>()
    @Volatile
    private var snapshot: SkillPluginSnapshot = SkillPluginSnapshot.empty()

    override fun bootstrap() {
        if (bootstrapped.get()) return
        synchronized(this) {
            if (bootstrapped.get()) return
            definitions += ContentModuleIndex.skillPlugins
            rebuildSnapshotLocked()
            bootstrapped.set(true)
        }
    }

    fun register(plugin: SkillPlugin) {
        synchronized(this) {
            definitions += plugin
            if (bootstrapped.get()) {
                rebuildSnapshotLocked()
            }
        }
    }

    fun current(): SkillPluginSnapshot {
        bootstrap()
        return snapshot
    }

    internal fun clearForTests() {
        synchronized(this) {
            definitions.clear()
            snapshot = SkillPluginSnapshot.empty()
            bootstrapped.set(true)
        }
    }

    internal fun resetForTests() {
        synchronized(this) {
            definitions.clear()
            snapshot = SkillPluginSnapshot.empty()
            bootstrapped.set(false)
        }
    }

    private fun rebuildSnapshotLocked() {
        val objectBindings = HashMap<Long, SkillObjectClickBinding>()
        val npcBindings = HashMap<Long, SkillNpcClickBinding>()
        val itemOnItemBindings = HashMap<Long, SkillItemOnItemBinding>()
        val buttonBindings = HashMap<Long, SkillButtonBinding>()

        definitions.forEach { plugin ->
            val definition = plugin.definition

            definition.objectBindings.forEach { binding ->
                binding.objectIds.forEach { objectId ->
                    val key = objectKey(binding.option, objectId)
                    val existing = objectBindings.putIfAbsent(key, binding)
                    require(existing == null) {
                        "Duplicate skill object binding option=${binding.option} objectId=$objectId " +
                            "for plugin=${definition.name}"
                    }
                }
            }

            definition.npcBindings.forEach { binding ->
                binding.npcIds.forEach { npcId ->
                    val key = npcKey(binding.option, npcId)
                    val existing = npcBindings.putIfAbsent(key, binding)
                    require(existing == null) {
                        "Duplicate skill npc binding option=${binding.option} npcId=$npcId " +
                            "for plugin=${definition.name}"
                    }
                }
            }

            definition.itemOnItemBindings.forEach { binding ->
                val key = itemPairKey(binding.leftItemId, binding.rightItemId)
                val existing = itemOnItemBindings.putIfAbsent(key, binding)
                require(existing == null) {
                    "Duplicate skill item-on-item binding left=${binding.leftItemId} right=${binding.rightItemId} " +
                        "for plugin=${definition.name}"
                }
            }

            definition.buttonBindings.forEach { binding ->
                binding.rawButtonIds.forEach { rawButtonId ->
                    val key = buttonKey(rawButtonId, binding.opIndex ?: -1)
                    val existing = buttonBindings.putIfAbsent(key, binding)
                    require(existing == null) {
                        "Duplicate skill button binding raw=$rawButtonId op=${binding.opIndex ?: -1} " +
                            "for plugin=${definition.name}"
                    }
                }
            }
        }

        snapshot = SkillPluginSnapshot(objectBindings, npcBindings, itemOnItemBindings, buttonBindings)
        logger.info(
            "SkillPluginRegistry bootstrapped {} plugins (object={}, npc={}, itemOnItem={}, button={})",
            definitions.size,
            objectBindings.size,
            npcBindings.size,
            itemOnItemBindings.size,
            buttonBindings.size,
        )
    }

    internal fun objectKey(option: Int, objectId: Int): Long {
        return (option.toLong() shl 32) or (objectId.toLong() and 0xffffffffL)
    }

    internal fun npcKey(option: Int, npcId: Int): Long {
        return (option.toLong() shl 32) or (npcId.toLong() and 0xffffffffL)
    }

    internal fun itemPairKey(a: Int, b: Int): Long {
        val left = minOf(a, b).toLong() and 0xffffffffL
        val right = maxOf(a, b).toLong() and 0xffffffffL
        return (left shl 32) or right
    }

    internal fun buttonKey(rawButtonId: Int, opIndex: Int): Long {
        return (rawButtonId.toLong() shl 32) or (opIndex.toLong() and 0xffffffffL)
    }
}

data class SkillPluginSnapshot(
    private val objectBindings: Map<Long, SkillObjectClickBinding>,
    private val npcBindings: Map<Long, SkillNpcClickBinding>,
    private val itemOnItemBindings: Map<Long, SkillItemOnItemBinding>,
    private val buttonBindings: Map<Long, SkillButtonBinding>,
) {
    fun objectBinding(option: Int, objectId: Int): SkillObjectClickBinding? =
        objectBindings[SkillPluginRegistry.objectKey(option, objectId)]

    fun npcBinding(option: Int, npcId: Int): SkillNpcClickBinding? =
        npcBindings[SkillPluginRegistry.npcKey(option, npcId)]

    fun itemOnItemBinding(itemUsed: Int, otherItem: Int): SkillItemOnItemBinding? =
        itemOnItemBindings[SkillPluginRegistry.itemPairKey(itemUsed, otherItem)]

    fun buttonBinding(rawButtonId: Int, opIndex: Int): SkillButtonBinding? {
        return buttonBindings[SkillPluginRegistry.buttonKey(rawButtonId, opIndex)]
            ?: buttonBindings[SkillPluginRegistry.buttonKey(rawButtonId, -1)]
    }

    companion object {
        @JvmStatic
        fun empty(): SkillPluginSnapshot = SkillPluginSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyMap())
    }
}
