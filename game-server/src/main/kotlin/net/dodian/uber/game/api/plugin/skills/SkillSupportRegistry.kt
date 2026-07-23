package net.dodian.uber.game.api.plugin.skills

import java.util.concurrent.atomic.AtomicBoolean

/** Engine registry for non-trainable skill content (for example Skill Guide). */
internal class SkillSupportRegistryEngine {
    private val frozen = AtomicBoolean(false)
    @Volatile private var snapshot = SkillSupportSnapshot.empty()

    fun bootstrap(modules: List<SkillSupportModule>) {
        if (frozen.get()) return
        synchronized(this) {
            if (frozen.get()) return
            snapshot = build(modules.sortedBy { it::class.java.name })
            frozen.set(true)
        }
    }

    fun current(): SkillSupportSnapshot = snapshot
    fun resetForTests() { synchronized(this) { snapshot = SkillSupportSnapshot.empty(); frozen.set(false) } }

    private fun build(modules: List<SkillSupportModule>): SkillSupportSnapshot {
        val items = HashMap<Long, SkillItemClickBinding>()
        val buttons = HashMap<Long, MutableList<SkillButtonBinding>>()
        val grids = HashMap<Int, SkillItemGridBinding>()
        modules.forEach { module ->
            module.definition.itemBindings.forEach { binding -> binding.itemIds.forEach { id ->
                require(items.putIfAbsent(SkillPluginKeys.itemKey(binding.option, id), binding) == null) {
                    "Duplicate support item binding option=${binding.option} itemId=$id module=${module.definition.name}"
                }
            } }
            module.definition.buttonBindings.forEach { binding -> binding.rawButtonIds.forEach { raw ->
                val key = SkillPluginKeys.buttonKey(raw, binding.opIndex ?: -1)
                val siblings = buttons.getOrPut(key) { mutableListOf() }
                require(siblings.none { it.requiredInterfaceId == binding.requiredInterfaceId }) {
                    "Duplicate support button binding raw=$raw op=${binding.opIndex ?: -1} interface=${binding.requiredInterfaceId} module=${module.definition.name}"
                }
                siblings += binding
            } }
            module.definition.itemGridBindings.forEach { binding ->
                require(grids.putIfAbsent(binding.interfaceId, binding) == null) {
                    "Duplicate support item-grid binding interfaceId=${binding.interfaceId} module=${module.definition.name}"
                }
            }
        }
        return SkillSupportSnapshot(items, buttons.mapValues { it.value.toList() }, grids)
    }
}

data class SkillSupportSnapshot(
    private val items: Map<Long, SkillItemClickBinding>,
    private val buttons: Map<Long, List<SkillButtonBinding>>,
    private val grids: Map<Int, SkillItemGridBinding>,
) {
    fun itemBinding(option: Int, itemId: Int) = items[SkillPluginKeys.itemKey(option, itemId)]
    fun buttonBinding(raw: Int, opIndex: Int, interfaceId: Int): SkillButtonBinding? {
        val exact = buttons[SkillPluginKeys.buttonKey(raw, opIndex)].orEmpty()
        val generic = buttons[SkillPluginKeys.buttonKey(raw, -1)].orEmpty()
        return (exact + generic).firstOrNull { it.requiredInterfaceId == -1 || it.requiredInterfaceId == interfaceId }
    }
    fun itemGridBinding(interfaceId: Int) = grids[interfaceId]
    fun routeKeys(): Set<String> = buildSet {
        items.forEach { (key, _) -> add("item:$key") }
        grids.keys.forEach { add("item-grid:$it") }
    }
    companion object { fun empty() = SkillSupportSnapshot(emptyMap(), emptyMap(), emptyMap()) }
}
