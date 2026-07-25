package net.dodian.uber.game.api.plugin.quests

import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

internal object QuestPluginKeys {
    fun objectKey(option: Int, objectId: Int): Long = (option.toLong() shl 32) or (objectId.toLong() and 0xffffffffL)
    fun npcKey(option: Int, npcId: Int): Long = (option.toLong() shl 32) or (npcId.toLong() and 0xffffffffL)
    fun itemPairKey(a: Int, b: Int): Long {
        val left = minOf(a, b).toLong() and 0xffffffffL
        val right = maxOf(a, b).toLong() and 0xffffffffL
        return (left shl 32) or right
    }
    fun itemKey(option: Int, itemId: Int): Long = (option.toLong() shl 32) or (itemId.toLong() and 0xffffffffL)
    fun itemOnObjectKey(objectId: Int, itemId: Int): Long = (objectId.toLong() shl 32) or (itemId.toLong() and 0xffffffffL)
    fun magicOnObjectKey(objectId: Int, spellId: Int): Long = (objectId.toLong() shl 32) or (spellId.toLong() and 0xffffffffL)
    fun buttonKey(rawButtonId: Int, opIndex: Int): Long = (rawButtonId.toLong() shl 32) or (opIndex.toLong() and 0xffffffffL)
}

/** Discover/validate/bootstrap/freeze binding engine for [QuestPlugin]s. Mirrors `SkillPluginRegistryEngine`. */
internal class QuestPluginRegistryEngine {
    private val logger = LoggerFactory.getLogger("PluginRegistry")

    private val bootstrapped = AtomicBoolean(false)
    private val frozen = AtomicBoolean(false)
    private val definitions = mutableListOf<QuestPlugin>()
    @Volatile
    private var snapshot: QuestPluginSnapshot = QuestPluginSnapshot.empty()

    fun validate(discoveredPlugins: List<QuestPlugin>) {
        synchronized(this) { buildSnapshot(discoveredPlugins.sortedBy { it::class.java.name }) }
    }

    fun bootstrap(discoveredPlugins: List<QuestPlugin>) {
        if (bootstrapped.get()) return
        synchronized(this) {
            if (bootstrapped.get()) return
            definitions += discoveredPlugins.sortedBy { it::class.java.name }
            rebuildSnapshotLocked()
            bootstrapped.set(true)
        }
    }

    fun freeze() { frozen.set(true) }

    fun register(plugin: QuestPlugin) {
        synchronized(this) {
            check(!frozen.get()) { "Quest plugin registry is frozen; cannot register ${plugin::class.java.name}" }
            definitions += plugin
            if (bootstrapped.get()) rebuildSnapshotLocked()
        }
    }

    fun current(): QuestPluginSnapshot = snapshot

    fun clearForTests() {
        synchronized(this) {
            definitions.clear()
            snapshot = QuestPluginSnapshot.empty()
            bootstrapped.set(true)
            frozen.set(true)
        }
    }

    fun resetForTests() {
        synchronized(this) {
            definitions.clear()
            snapshot = QuestPluginSnapshot.empty()
            bootstrapped.set(false)
            frozen.set(false)
        }
    }

    private fun rebuildSnapshotLocked() {
        snapshot = buildSnapshot(definitions)
        logger.info(
            "quests bootstrapped {} plugins [{}] (object={}, npc={}, itemOnItem={}, item={}, itemOnObject={}, magicOnObject={}, button={})",
            definitions.size,
            definitions.joinToString { it.definition.name },
            snapshot.objectBindingCount,
            snapshot.npcBindingCount,
            snapshot.itemOnItemBindingCount,
            snapshot.itemBindingCount,
            snapshot.itemOnObjectBindingCount,
            snapshot.magicOnObjectBindingCount,
            snapshot.buttonBindingCount,
        )
    }

    private fun buildSnapshot(source: List<QuestPlugin>): QuestPluginSnapshot {
        val objectBindings = HashMap<Long, QuestObjectClickBinding>()
        val npcBindings = HashMap<Long, QuestNpcClickBinding>()
        val itemOnItemBindings = HashMap<Long, QuestItemOnItemBinding>()
        val itemBindings = HashMap<Long, QuestItemClickBinding>()
        val itemOnObjectBindings = HashMap<Long, QuestItemOnObjectBinding>()
        val magicOnObjectBindings = HashMap<Long, QuestMagicOnObjectBinding>()
        val buttonBindings = HashMap<Long, MutableList<QuestButtonBinding>>()

        source.forEach { plugin ->
            val definition = plugin.definition
            require(definition.name.isNotBlank()) { "Quest plugin ${plugin::class.java.name} has a blank name" }

            definition.objectBindings.forEach { binding ->
                binding.objectIds.forEach { objectId ->
                    val key = QuestPluginKeys.objectKey(binding.option, objectId)
                    require(objectBindings.putIfAbsent(key, binding) == null) {
                        "Duplicate quest object binding option=${binding.option} objectId=$objectId for plugin=${definition.name}"
                    }
                }
            }
            definition.npcBindings.forEach { binding ->
                binding.npcIds.forEach { npcId ->
                    val key = QuestPluginKeys.npcKey(binding.option, npcId)
                    require(npcBindings.putIfAbsent(key, binding) == null) {
                        "Duplicate quest npc binding option=${binding.option} npcId=$npcId for plugin=${definition.name}"
                    }
                }
            }
            definition.itemOnItemBindings.forEach { binding ->
                val key = QuestPluginKeys.itemPairKey(binding.leftItemId, binding.rightItemId)
                require(itemOnItemBindings.putIfAbsent(key, binding) == null) {
                    "Duplicate quest item-on-item binding left=${binding.leftItemId} right=${binding.rightItemId} for plugin=${definition.name}"
                }
            }
            definition.itemBindings.forEach { binding ->
                binding.itemIds.forEach { itemId ->
                    val key = QuestPluginKeys.itemKey(binding.option, itemId)
                    require(itemBindings.putIfAbsent(key, binding) == null) {
                        "Duplicate quest item binding option=${binding.option} itemId=$itemId for plugin=${definition.name}"
                    }
                }
            }
            definition.itemOnObjectBindings.forEach { binding ->
                binding.objectIds.forEach { objectId ->
                    binding.itemIds.forEach { itemId ->
                        val key = QuestPluginKeys.itemOnObjectKey(objectId, itemId)
                        require(itemOnObjectBindings.putIfAbsent(key, binding) == null) {
                            "Duplicate quest item-on-object binding objectId=$objectId itemId=$itemId for plugin=${definition.name}"
                        }
                    }
                }
            }
            definition.magicOnObjectBindings.forEach { binding ->
                binding.objectIds.forEach { objectId ->
                    binding.spellIds.forEach { spellId ->
                        val key = QuestPluginKeys.magicOnObjectKey(objectId, spellId)
                        require(magicOnObjectBindings.putIfAbsent(key, binding) == null) {
                            "Duplicate quest magic-on-object binding objectId=$objectId spellId=$spellId for plugin=${definition.name}"
                        }
                    }
                }
            }
            definition.buttonBindings.forEach { binding ->
                binding.rawButtonIds.forEach { rawButtonId ->
                    val key = QuestPluginKeys.buttonKey(rawButtonId, binding.opIndex ?: -1)
                    val siblings = buttonBindings.getOrPut(key) { mutableListOf() }
                    require(siblings.none { it.requiredInterfaceId == binding.requiredInterfaceId }) {
                        "Duplicate quest button binding raw=$rawButtonId op=${binding.opIndex ?: -1} requiredInterfaceId=${binding.requiredInterfaceId} for plugin=${definition.name}"
                    }
                    siblings += binding
                }
            }
        }

        return QuestPluginSnapshot(
            objectBindings = objectBindings,
            npcBindings = npcBindings,
            itemOnItemBindings = itemOnItemBindings,
            itemBindings = itemBindings,
            itemOnObjectBindings = itemOnObjectBindings,
            magicOnObjectBindings = magicOnObjectBindings,
            buttonBindings = buttonBindings.mapValues { it.value.toList() },
        )
    }
}

data class QuestPluginSnapshot(
    private val objectBindings: Map<Long, QuestObjectClickBinding>,
    private val npcBindings: Map<Long, QuestNpcClickBinding>,
    private val itemOnItemBindings: Map<Long, QuestItemOnItemBinding>,
    private val itemBindings: Map<Long, QuestItemClickBinding>,
    private val itemOnObjectBindings: Map<Long, QuestItemOnObjectBinding>,
    private val magicOnObjectBindings: Map<Long, QuestMagicOnObjectBinding>,
    private val buttonBindings: Map<Long, List<QuestButtonBinding>>,
) {
    val objectBindingCount: Int get() = objectBindings.size
    val npcBindingCount: Int get() = npcBindings.size
    val itemOnItemBindingCount: Int get() = itemOnItemBindings.size
    val itemBindingCount: Int get() = itemBindings.size
    val itemOnObjectBindingCount: Int get() = itemOnObjectBindings.size
    val magicOnObjectBindingCount: Int get() = magicOnObjectBindings.size
    val buttonBindingCount: Int get() = buttonBindings.values.sumOf { it.size }

    fun objectBinding(option: Int, objectId: Int): QuestObjectClickBinding? = objectBindings[QuestPluginKeys.objectKey(option, objectId)]
    fun npcBinding(option: Int, npcId: Int): QuestNpcClickBinding? = npcBindings[QuestPluginKeys.npcKey(option, npcId)]
    fun itemOnItemBinding(itemUsed: Int, otherItem: Int): QuestItemOnItemBinding? = itemOnItemBindings[QuestPluginKeys.itemPairKey(itemUsed, otherItem)]
    fun itemBinding(option: Int, itemId: Int): QuestItemClickBinding? = itemBindings[QuestPluginKeys.itemKey(option, itemId)]
    fun itemOnObjectBinding(objectId: Int, itemId: Int): QuestItemOnObjectBinding? =
        itemOnObjectBindings[QuestPluginKeys.itemOnObjectKey(objectId, itemId)]
            ?: itemOnObjectBindings[QuestPluginKeys.itemOnObjectKey(objectId, -1)]
    fun magicOnObjectBinding(objectId: Int, spellId: Int): QuestMagicOnObjectBinding? =
        magicOnObjectBindings[QuestPluginKeys.magicOnObjectKey(objectId, spellId)]
            ?: magicOnObjectBindings[QuestPluginKeys.magicOnObjectKey(objectId, -1)]
    fun buttonBinding(rawButtonId: Int, opIndex: Int, activeInterfaceId: Int): QuestButtonBinding? =
        resolveButtonBinding(rawButtonId, opIndex, activeInterfaceId) ?: resolveButtonBinding(rawButtonId, -1, activeInterfaceId)

    private fun resolveButtonBinding(rawButtonId: Int, opIndex: Int, activeInterfaceId: Int): QuestButtonBinding? {
        val bindings = buttonBindings[QuestPluginKeys.buttonKey(rawButtonId, opIndex)] ?: return null
        return bindings.firstOrNull { it.requiredInterfaceId == activeInterfaceId } ?: bindings.firstOrNull { it.requiredInterfaceId == -1 }
    }

    companion object {
        @JvmStatic
        fun empty(): QuestPluginSnapshot = QuestPluginSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap())
    }
}
