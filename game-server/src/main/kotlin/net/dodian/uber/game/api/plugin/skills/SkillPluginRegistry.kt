package net.dodian.uber.game.api.plugin.skills

import net.dodian.uber.game.api.plugin.ContentBootstrap
import net.dodian.uber.game.api.plugin.PluginRegistry
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

@Deprecated("Use net.dodian.uber.game.api.plugin.PluginRegistry")
object SkillPluginRegistry : ContentBootstrap {
    override val id: String = "skills.registry"

    override fun bootstrap() = PluginRegistry.bootstrap()

    fun register(plugin: SkillPlugin) = PluginRegistry.registerSkill(plugin)

    fun current(): SkillPluginSnapshot = PluginRegistry.currentSkills()

    internal fun clearForTests() = PluginRegistry.clearForTests()

    internal fun resetForTests() = PluginRegistry.resetForTests()
}

internal object SkillPluginKeys {
    fun objectKey(option: Int, objectId: Int): Long {
        return (option.toLong() shl 32) or (objectId.toLong() and 0xffffffffL)
    }

    fun npcKey(option: Int, npcId: Int): Long {
        return (option.toLong() shl 32) or (npcId.toLong() and 0xffffffffL)
    }

    fun itemPairKey(a: Int, b: Int): Long {
        val left = minOf(a, b).toLong() and 0xffffffffL
        val right = maxOf(a, b).toLong() and 0xffffffffL
        return (left shl 32) or right
    }

    fun itemKey(option: Int, itemId: Int): Long {
        return (option.toLong() shl 32) or (itemId.toLong() and 0xffffffffL)
    }

    fun itemOnObjectKey(objectId: Int, itemId: Int): Long {
        return (objectId.toLong() shl 32) or (itemId.toLong() and 0xffffffffL)
    }

    fun magicOnObjectKey(objectId: Int, spellId: Int): Long {
        return (objectId.toLong() shl 32) or (spellId.toLong() and 0xffffffffL)
    }

    fun buttonKey(rawButtonId: Int, opIndex: Int): Long {
        return (rawButtonId.toLong() shl 32) or (opIndex.toLong() and 0xffffffffL)
    }
}

internal class SkillPluginRegistryEngine {
    private val logger = LoggerFactory.getLogger("PluginRegistry")

    private val bootstrapped = AtomicBoolean(false)
    private val frozen = AtomicBoolean(false)
    private val definitions = mutableListOf<SkillPlugin>()
    @Volatile
    private var snapshot: SkillPluginSnapshot = SkillPluginSnapshot.empty()

    fun validate(discoveredPlugins: List<SkillPlugin>) {
        synchronized(this) {
            buildSnapshot(discoveredPlugins.sortedBy { it::class.java.name })
        }
    }

    fun bootstrap(discoveredPlugins: List<SkillPlugin>) {
        if (bootstrapped.get()) return
        synchronized(this) {
            if (bootstrapped.get()) return
            definitions += discoveredPlugins.sortedBy { it::class.java.name }
            rebuildSnapshotLocked()
            bootstrapped.set(true)
        }
    }

    fun freeze() {
        frozen.set(true)
    }

    fun register(plugin: SkillPlugin) {
        synchronized(this) {
            check(!frozen.get()) { "Skill plugin registry is frozen; cannot register ${plugin::class.java.name}" }
            definitions += plugin
            if (bootstrapped.get()) {
                rebuildSnapshotLocked()
            }
        }
    }

    fun current(): SkillPluginSnapshot {
        return snapshot
    }

    fun clearForTests() {
        synchronized(this) {
            definitions.clear()
            snapshot = SkillPluginSnapshot.empty()
            bootstrapped.set(true)
            frozen.set(true)
        }
    }

    fun resetForTests() {
        synchronized(this) {
            definitions.clear()
            snapshot = SkillPluginSnapshot.empty()
            bootstrapped.set(false)
            frozen.set(false)
        }
    }

    private fun rebuildSnapshotLocked() {
        snapshot = buildSnapshot(definitions)
        logger.info(
            "skills bootstrapped {} plugins (object={}, npc={}, itemOnItem={}, item={}, itemOnObject={}, magicOnObject={}, button={})",
            definitions.size,
            snapshot.objectBindingCount,
            snapshot.npcBindingCount,
            snapshot.itemOnItemBindingCount,
            snapshot.itemBindingCount,
            snapshot.itemOnObjectBindingCount,
            snapshot.magicOnObjectBindingCount,
            snapshot.buttonBindingCount,
        )
    }

    private fun buildSnapshot(source: List<SkillPlugin>): SkillPluginSnapshot {
        val objectBindings = HashMap<Long, SkillObjectClickBinding>()
        val npcBindings = HashMap<Long, SkillNpcClickBinding>()
        val itemOnItemBindings = HashMap<Long, SkillItemOnItemBinding>()
        val itemBindings = HashMap<Long, SkillItemClickBinding>()
        val itemOnObjectBindings = HashMap<Long, SkillItemOnObjectBinding>()
        val magicOnObjectBindings = HashMap<Long, SkillMagicOnObjectBinding>()
        val buttonBindings = HashMap<Long, MutableList<SkillButtonBinding>>()
        val objectPresetById = HashMap<Int, MutableSet<net.dodian.uber.game.engine.systems.action.PolicyPreset>>()

        source.forEach { plugin ->
            val definition = plugin.definition

            definition.objectBindings.forEach { binding ->
                binding.objectIds.forEach { objectId ->
                    val key = SkillPluginKeys.objectKey(binding.option, objectId)
                    val existing = objectBindings.putIfAbsent(key, binding)
                    require(existing == null) {
                        "Duplicate skill object binding option=${binding.option} objectId=$objectId " +
                            "for plugin=${definition.name}"
                    }
                    objectPresetById.computeIfAbsent(objectId) { linkedSetOf() }.add(binding.preset)
                }
            }

            definition.npcBindings.forEach { binding ->
                binding.npcIds.forEach { npcId ->
                    val key = SkillPluginKeys.npcKey(binding.option, npcId)
                    val existing = npcBindings.putIfAbsent(key, binding)
                    require(existing == null) {
                        "Duplicate skill npc binding option=${binding.option} npcId=$npcId " +
                            "for plugin=${definition.name}"
                    }
                }
            }

            definition.itemOnItemBindings.forEach { binding ->
                val key = SkillPluginKeys.itemPairKey(binding.leftItemId, binding.rightItemId)
                val existing = itemOnItemBindings.putIfAbsent(key, binding)
                require(existing == null) {
                    "Duplicate skill item-on-item binding left=${binding.leftItemId} right=${binding.rightItemId} " +
                        "for plugin=${definition.name}"
                }
            }

            definition.itemBindings.forEach { binding ->
                binding.itemIds.forEach { itemId ->
                    val key = SkillPluginKeys.itemKey(binding.option, itemId)
                    val existing = itemBindings.putIfAbsent(key, binding)
                    require(existing == null) {
                        "Duplicate skill item binding option=${binding.option} itemId=$itemId " +
                            "for plugin=${definition.name}"
                    }
                }
            }

            definition.itemOnObjectBindings.forEach { binding ->
                binding.objectIds.forEach { objectId ->
                    binding.itemIds.forEach { itemId ->
                        val key = SkillPluginKeys.itemOnObjectKey(objectId, itemId)
                        val existing = itemOnObjectBindings.putIfAbsent(key, binding)
                        require(existing == null) {
                            "Duplicate skill item-on-object binding objectId=$objectId itemId=$itemId " +
                                "for plugin=${definition.name}"
                        }
                    }
                }
            }

            definition.magicOnObjectBindings.forEach { binding ->
                binding.objectIds.forEach { objectId ->
                    binding.spellIds.forEach { spellId ->
                        val key = SkillPluginKeys.magicOnObjectKey(objectId, spellId)
                        val existing = magicOnObjectBindings.putIfAbsent(key, binding)
                        require(existing == null) {
                            "Duplicate skill magic-on-object binding objectId=$objectId spellId=$spellId " +
                                "for plugin=${definition.name}"
                        }
                        objectPresetById.computeIfAbsent(objectId) { linkedSetOf() }.add(binding.preset)
                    }
                }
            }

            definition.buttonBindings.forEach { binding ->
                binding.rawButtonIds.forEach { rawButtonId ->
                    val key = SkillPluginKeys.buttonKey(rawButtonId, binding.opIndex ?: -1)
                    val siblings = buttonBindings.getOrPut(key) { mutableListOf() }
                    val existing = siblings.firstOrNull { it.requiredInterfaceId == binding.requiredInterfaceId }
                    require(existing == null) {
                        "Duplicate skill button binding raw=$rawButtonId op=${binding.opIndex ?: -1} " +
                            "requiredInterfaceId=${binding.requiredInterfaceId} for plugin=${definition.name}"
                    }
                    siblings += binding
                }
            }
        }

        return SkillPluginSnapshot(
            objectBindings = objectBindings,
            npcBindings = npcBindings,
            itemOnItemBindings = itemOnItemBindings,
            itemBindings = itemBindings,
            itemOnObjectBindings = itemOnObjectBindings,
            magicOnObjectBindings = magicOnObjectBindings,
            buttonBindings = buttonBindings.mapValues { it.value.toList() },
            objectPresetById = objectPresetById,
        )
    }
}

data class SkillPluginSnapshot(
    private val objectBindings: Map<Long, SkillObjectClickBinding>,
    private val npcBindings: Map<Long, SkillNpcClickBinding>,
    private val itemOnItemBindings: Map<Long, SkillItemOnItemBinding>,
    private val itemBindings: Map<Long, SkillItemClickBinding>,
    private val itemOnObjectBindings: Map<Long, SkillItemOnObjectBinding>,
    private val magicOnObjectBindings: Map<Long, SkillMagicOnObjectBinding>,
    private val buttonBindings: Map<Long, List<SkillButtonBinding>>,
    private val objectPresetById: Map<Int, Set<net.dodian.uber.game.engine.systems.action.PolicyPreset>>,
) {
    val objectBindingCount: Int get() = objectBindings.size
    val npcBindingCount: Int get() = npcBindings.size
    val itemOnItemBindingCount: Int get() = itemOnItemBindings.size
    val itemBindingCount: Int get() = itemBindings.size
    val itemOnObjectBindingCount: Int get() = itemOnObjectBindings.size
    val magicOnObjectBindingCount: Int get() = magicOnObjectBindings.size
    val buttonBindingCount: Int get() = buttonBindings.values.sumOf { it.size }

    fun objectBinding(option: Int, objectId: Int): SkillObjectClickBinding? =
        objectBindings[SkillPluginKeys.objectKey(option, objectId)]

    fun npcBinding(option: Int, npcId: Int): SkillNpcClickBinding? =
        npcBindings[SkillPluginKeys.npcKey(option, npcId)]

    fun itemOnItemBinding(itemUsed: Int, otherItem: Int): SkillItemOnItemBinding? =
        itemOnItemBindings[SkillPluginKeys.itemPairKey(itemUsed, otherItem)]

    fun itemBinding(option: Int, itemId: Int): SkillItemClickBinding? =
        itemBindings[SkillPluginKeys.itemKey(option, itemId)]

    fun itemOnObjectBinding(objectId: Int, itemId: Int): SkillItemOnObjectBinding? =
        itemOnObjectBindings[SkillPluginKeys.itemOnObjectKey(objectId, itemId)]
            ?: itemOnObjectBindings[SkillPluginKeys.itemOnObjectKey(objectId, -1)]

    fun magicOnObjectBinding(objectId: Int, spellId: Int): SkillMagicOnObjectBinding? =
        magicOnObjectBindings[SkillPluginKeys.magicOnObjectKey(objectId, spellId)]
            ?: magicOnObjectBindings[SkillPluginKeys.magicOnObjectKey(objectId, -1)]

    fun buttonBinding(rawButtonId: Int, opIndex: Int, activeInterfaceId: Int): SkillButtonBinding? {
        return resolveButtonBinding(rawButtonId, opIndex, activeInterfaceId)
            ?: resolveButtonBinding(rawButtonId, -1, activeInterfaceId)
    }

    fun ownsObjectId(objectId: Int): Boolean = objectPresetById.containsKey(objectId)

    fun firstPresetForObjectId(objectId: Int): net.dodian.uber.game.engine.systems.action.PolicyPreset? =
        objectPresetById[objectId]?.firstOrNull()

    companion object {
        @JvmStatic
        fun empty(): SkillPluginSnapshot =
            SkillPluginSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap())
    }

    private fun resolveButtonBinding(rawButtonId: Int, opIndex: Int, activeInterfaceId: Int): SkillButtonBinding? {
        val bindings = buttonBindings[SkillPluginKeys.buttonKey(rawButtonId, opIndex)] ?: return null
        return bindings.firstOrNull { it.requiredInterfaceId == activeInterfaceId }
            ?: bindings.firstOrNull { it.requiredInterfaceId == -1 }
    }
}
