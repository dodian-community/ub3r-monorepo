package net.dodian.uber.game.api.plugin.skills

import net.dodian.uber.game.api.plugin.*
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.model.player.skills.Skill

/**
 * Common contract for Gradle-owned skill content.  A module may expose route
 * bindings without representing a trainable skill (for example Skill Guide).
 */
interface SkillContentModule : PluginModuleMetadataProvider, ContentModuleManifestProvider

interface SkillPlugin : SkillContentModule {
    val definition: SkillPluginDefinition
    override val pluginMetadata get() = PluginModuleMetadata(definition.name, "Skill plugin for ${definition.skill.name.lowercase()}", owner = "gameplay")
    override val contentManifest get() = definition.manifest("skill.${definition.skill.name.lowercase()}", "gameplay")
}

data class SkillPluginDefinition(
    val name: String, val skill: Skill,
    val objectBindings: List<SkillObjectClickBinding> = emptyList(),
    val npcBindings: List<SkillNpcClickBinding> = emptyList(),
    val itemOnItemBindings: List<SkillItemOnItemBinding> = emptyList(),
    val itemBindings: List<SkillItemClickBinding> = emptyList(),
    val itemOnObjectBindings: List<SkillItemOnObjectBinding> = emptyList(),
    val magicOnObjectBindings: List<SkillMagicOnObjectBinding> = emptyList(),
    val buttonBindings: List<SkillButtonBinding> = emptyList(),
    val itemGridBindings: List<SkillItemGridBinding> = emptyList(),
    val lifecycle: SkillPluginLifecycleHooks = SkillPluginLifecycleHooks(),
)

fun SkillPluginDefinition.routeKeys(): Set<String> = buildSet {
    objectBindings.forEach { b -> b.objectIds.forEach { add("object:${b.option}:$it") } }
    npcBindings.forEach { b -> b.npcIds.forEach { add("npc:${b.option}:$it") } }
    itemBindings.forEach { b -> b.itemIds.forEach { add("item:${b.option}:$it") } }
    itemOnItemBindings.forEach { add("item-on-item:${it.leftItemId}:${it.rightItemId}") }
    itemOnObjectBindings.forEach { b -> b.objectIds.forEach { o -> b.itemIds.forEach { add("item-on-object:$o:$it") } } }
    magicOnObjectBindings.forEach { b -> b.objectIds.forEach { o -> b.spellIds.forEach { add("magic-on-object:$o:$it") } } }
    buttonBindings.forEach { b -> b.rawButtonIds.forEach { add("button:$it:${b.opIndex ?: -1}:${b.requiredInterfaceId}") } }
    itemGridBindings.forEach { add("item-grid:${it.interfaceId}") }
}
fun SkillPluginDefinition.manifest(id: String, owner: String, version: String = "1.0.0", featureFlag: String = ContentModuleManifest.ALWAYS_ENABLED, maturity: ContentMaturity = ContentMaturity.BETA) =
    ContentModuleManifest(id, owner, version, featureFlag, maturity, routeKeys())

data class SkillPluginLifecycleHooks(val onAttempt: ((SkillPlayer) -> Unit)? = null, val onStart: ((SkillPlayer) -> Unit)? = null, val onCycle: ((SkillPlayer) -> Unit)? = null, val onStop: ((SkillPlayer) -> Unit)? = null)
data class SkillObjectClickBinding(val preset: PolicyPreset, val option: Int, val objectIds: IntArray, val handler: (SkillObjectInteraction) -> Boolean)
data class SkillNpcClickBinding(val preset: PolicyPreset, val option: Int, val npcIds: IntArray, val handler: (SkillNpcInteraction) -> Boolean)
data class SkillItemOnItemBinding(val preset: PolicyPreset, val leftItemId: Int, val rightItemId: Int, val handler: (SkillItemOnItemInteraction) -> Boolean)
data class SkillItemClickBinding(val preset: PolicyPreset, val option: Int, val itemIds: IntArray, val handler: (SkillItemInteraction) -> Boolean)
data class SkillItemOnObjectBinding(val preset: PolicyPreset, val objectIds: IntArray, val itemIds: IntArray, val handler: (SkillItemOnObjectInteraction) -> Boolean)
data class SkillMagicOnObjectBinding(val preset: PolicyPreset, val objectIds: IntArray, val spellIds: IntArray, val handler: (SkillMagicOnObjectInteraction) -> Boolean)
data class SkillButtonBinding(val preset: PolicyPreset, val rawButtonIds: IntArray, val requiredInterfaceId: Int = -1, val opIndex: Int? = null, val handler: (SkillButtonInteraction) -> Boolean)
data class SkillItemGridBinding(val preset: PolicyPreset, val interfaceId: Int, val handler: (SkillItemGridInteraction) -> Boolean)
