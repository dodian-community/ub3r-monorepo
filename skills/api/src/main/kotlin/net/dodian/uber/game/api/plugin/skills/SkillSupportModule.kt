package net.dodian.uber.game.api.plugin.skills

import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.PluginModuleMetadata
import net.dodian.uber.game.engine.systems.action.PolicyPreset

/**
 * Route-owning content that accompanies skills but is not itself trainable.
 * Skill Guide uses this contract so its book and interface controls remain
 * independently packaged without declaring a fake [SkillPlugin].
 */
interface SkillSupportModule : SkillContentModule {
    val definition: SkillSupportDefinition
    override val pluginMetadata get() = PluginModuleMetadata(definition.name, "Skill support module", owner = "support")
    override val contentManifest get() = definition.manifest("skill-support.${definition.name}", "support")
}

data class SkillSupportDefinition(
    val name: String,
    val itemBindings: List<SkillItemClickBinding> = emptyList(),
    val buttonBindings: List<SkillButtonBinding> = emptyList(),
    val itemGridBindings: List<SkillItemGridBinding> = emptyList(),
)

fun SkillSupportDefinition.routeKeys(): Set<String> = buildSet {
    itemBindings.forEach { binding -> binding.itemIds.forEach { add("item:${binding.option}:$it") } }
    buttonBindings.forEach { binding -> binding.rawButtonIds.forEach { add("button:$it:${binding.opIndex ?: -1}:${binding.requiredInterfaceId}") } }
    itemGridBindings.forEach { add("item-grid:${it.interfaceId}") }
}

fun SkillSupportDefinition.manifest(
    id: String,
    owner: String,
    version: String = "1.0.0",
    maturity: ContentMaturity = ContentMaturity.BETA,
) = ContentModuleManifest(id, owner, version, ContentModuleManifest.ALWAYS_ENABLED, maturity, routeKeys())

class SkillSupportModuleBuilder internal constructor(private val name: String) {
    private val items = mutableListOf<SkillItemClickBinding>()
    private val buttons = mutableListOf<SkillButtonBinding>()
    private val itemGrids = mutableListOf<SkillItemGridBinding>()

    fun itemClick(preset: PolicyPreset, option: Int, vararg itemIds: Int, handler: (SkillItemInteraction) -> Boolean) {
        require(option in 1..3 && itemIds.isNotEmpty())
        items += SkillItemClickBinding(preset, option, itemIds.distinct().toIntArray(), handler)
    }
    fun button(preset: PolicyPreset, requiredInterfaceId: Int = -1, opIndex: Int? = null, vararg rawButtonIds: Int, handler: (SkillButtonInteraction) -> Boolean) {
        require(rawButtonIds.isNotEmpty() && requiredInterfaceId >= -1)
        buttons += SkillButtonBinding(preset, rawButtonIds.distinct().toIntArray(), requiredInterfaceId, opIndex, handler)
    }
    fun itemGrid(preset: PolicyPreset, interfaceId: Int, handler: (SkillItemGridInteraction) -> Boolean) {
        require(interfaceId > 0)
        itemGrids += SkillItemGridBinding(preset, interfaceId, handler)
    }
    internal fun build() = SkillSupportDefinition(name, items, buttons, itemGrids)
}

fun skillSupportModule(name: String, block: SkillSupportModuleBuilder.() -> Unit): SkillSupportDefinition =
    SkillSupportModuleBuilder(name).apply(block).build()
