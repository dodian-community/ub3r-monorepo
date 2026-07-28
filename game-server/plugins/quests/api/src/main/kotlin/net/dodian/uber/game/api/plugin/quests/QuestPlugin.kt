package net.dodian.uber.game.api.plugin.quests

import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.ContentModuleManifestProvider
import net.dodian.uber.game.api.plugin.PluginModuleMetadata
import net.dodian.uber.game.api.plugin.PluginModuleMetadataProvider
import net.dodian.uber.game.api.plugin.skills.SkillNpcRef
import net.dodian.uber.game.api.plugin.skills.SkillObjectRef

/**
 * Quest-scoped interaction events. These mirror the shape of
 * `SkillObjectInteraction`/`SkillNpcInteraction`/etc. from skills/api, but carry
 * a [QuestPlayer] instead of a plain `SkillPlayer` - quest handlers need
 * `player.questProgress`, which only exists on [QuestPlayer], so the skill
 * interaction types (whose `player` field is statically a `SkillPlayer`) can't
 * be reused here without an unsafe cast.
 */
class QuestObjectInteraction(val player: QuestPlayer, val option: Int, val target: SkillObjectRef) {
    val objectId get() = target.id
    val position get() = target.position
}
class QuestNpcInteraction(val player: QuestPlayer, val option: Int, val npc: SkillNpcRef)
class QuestItemOnItemInteraction(val player: QuestPlayer, val itemUsed: Int, val otherItem: Int)
class QuestItemInteraction(val player: QuestPlayer, val option: Int, val itemId: Int, val itemSlot: Int, val interfaceId: Int)
class QuestItemOnObjectInteraction(val player: QuestPlayer, val target: SkillObjectRef, val itemId: Int, val itemSlot: Int, val interfaceId: Int) {
    val objectId get() = target.id
    val position get() = target.position
}
class QuestMagicOnObjectInteraction(val player: QuestPlayer, val target: SkillObjectRef, val spellId: Int) {
    val objectId get() = target.id
    val position get() = target.position
}
class QuestButtonInteraction(val player: QuestPlayer, val rawButtonId: Int, val opIndex: Int, val activeInterfaceId: Int)

/** Common contract for Gradle-owned quest content. Mirrors `SkillPlugin`. */
interface QuestContentModule : PluginModuleMetadataProvider, ContentModuleManifestProvider

interface QuestPlugin : QuestContentModule {
    val definition: QuestPluginDefinition
    override val pluginMetadata get() = PluginModuleMetadata(definition.name, "Quest plugin: ${definition.name}", owner = "gameplay")
    override val contentManifest get() = definition.manifest("quest.${definition.name.lowercase().replace(' ', '_')}", "gameplay")
}

data class QuestPluginDefinition(
    val name: String,
    val objectBindings: List<QuestObjectClickBinding> = emptyList(),
    val npcBindings: List<QuestNpcClickBinding> = emptyList(),
    val itemOnItemBindings: List<QuestItemOnItemBinding> = emptyList(),
    val itemBindings: List<QuestItemClickBinding> = emptyList(),
    val itemOnObjectBindings: List<QuestItemOnObjectBinding> = emptyList(),
    val magicOnObjectBindings: List<QuestMagicOnObjectBinding> = emptyList(),
    val buttonBindings: List<QuestButtonBinding> = emptyList(),
)

fun QuestPluginDefinition.routeKeys(): Set<String> = buildSet {
    objectBindings.forEach { b -> b.objectIds.forEach { add("object:${b.option}:$it") } }
    npcBindings.forEach { b -> b.npcIds.forEach { add("npc:${b.option}:$it") } }
    itemBindings.forEach { b -> b.itemIds.forEach { add("item:${b.option}:$it") } }
    itemOnItemBindings.forEach { add("item-on-item:${minOf(it.leftItemId, it.rightItemId)}:${maxOf(it.leftItemId, it.rightItemId)}") }
    itemOnObjectBindings.forEach { b -> b.objectIds.forEach { o -> b.itemIds.forEach { add("item-on-object:$o:$it") } } }
    magicOnObjectBindings.forEach { b -> b.objectIds.forEach { o -> b.spellIds.forEach { add("magic-on-object:$o:$it") } } }
    buttonBindings.forEach { b -> b.rawButtonIds.forEach { add("button:$it:${b.opIndex ?: -1}:${b.requiredInterfaceId}") } }
}

fun QuestPluginDefinition.manifest(
    id: String,
    owner: String,
    version: String = "1.0.0",
    featureFlag: String = ContentModuleManifest.ALWAYS_ENABLED,
    maturity: ContentMaturity = ContentMaturity.BETA,
) = ContentModuleManifest(id, owner, version, featureFlag, maturity, routeKeys())

data class QuestObjectClickBinding(val option: Int, val objectIds: IntArray, val handler: (QuestObjectInteraction) -> Boolean)
data class QuestNpcClickBinding(val option: Int, val npcIds: IntArray, val handler: (QuestNpcInteraction) -> Boolean)
data class QuestItemOnItemBinding(val leftItemId: Int, val rightItemId: Int, val handler: (QuestItemOnItemInteraction) -> Boolean)
data class QuestItemClickBinding(val option: Int, val itemIds: IntArray, val handler: (QuestItemInteraction) -> Boolean)
data class QuestItemOnObjectBinding(val objectIds: IntArray, val itemIds: IntArray, val handler: (QuestItemOnObjectInteraction) -> Boolean)
data class QuestMagicOnObjectBinding(val objectIds: IntArray, val spellIds: IntArray, val handler: (QuestMagicOnObjectInteraction) -> Boolean)
data class QuestButtonBinding(val rawButtonIds: IntArray, val requiredInterfaceId: Int = -1, val opIndex: Int? = null, val handler: (QuestButtonInteraction) -> Boolean)

/** Route-registration DSL for quest content. Structurally mirrors `SkillPluginBuilder`, minus the `Skill` tag - quests aren't trainable skills. */
class QuestPluginBuilder internal constructor(private val name: String) {
    private val objects = mutableListOf<QuestObjectClickBinding>()
    private val npcs = mutableListOf<QuestNpcClickBinding>()
    private val pairs = mutableListOf<QuestItemOnItemBinding>()
    private val items = mutableListOf<QuestItemClickBinding>()
    private val itemObjects = mutableListOf<QuestItemOnObjectBinding>()
    private val magicObjects = mutableListOf<QuestMagicOnObjectBinding>()
    private val buttons = mutableListOf<QuestButtonBinding>()

    fun objectClick(option: Int = 1, vararg objectIds: Int, handler: (QuestObjectInteraction) -> Boolean) {
        require(option in 1..5 && objectIds.isNotEmpty())
        objects += QuestObjectClickBinding(option, objectIds.distinct().toIntArray(), handler)
    }

    fun npcClick(option: Int, vararg npcIds: Int, handler: (QuestNpcInteraction) -> Boolean) {
        require(option in 1..4 && npcIds.isNotEmpty())
        npcs += QuestNpcClickBinding(option, npcIds.distinct().toIntArray(), handler)
    }

    fun itemOnItem(leftItemId: Int, rightItemId: Int, handler: (QuestItemOnItemInteraction) -> Boolean) {
        require(leftItemId >= 0 && rightItemId >= 0)
        pairs += QuestItemOnItemBinding(leftItemId, rightItemId, handler)
    }

    fun itemClick(option: Int, vararg itemIds: Int, handler: (QuestItemInteraction) -> Boolean) {
        require(option in 1..3 && itemIds.isNotEmpty())
        items += QuestItemClickBinding(option, itemIds.distinct().toIntArray(), handler)
    }

    fun itemOnObject(vararg objectIds: Int, itemIds: IntArray = intArrayOf(-1), handler: (QuestItemOnObjectInteraction) -> Boolean) {
        require(objectIds.isNotEmpty() && itemIds.isNotEmpty())
        itemObjects += QuestItemOnObjectBinding(objectIds.distinct().toIntArray(), itemIds.distinct().toIntArray(), handler)
    }

    fun magicOnObject(vararg objectIds: Int, spellIds: IntArray = intArrayOf(-1), handler: (QuestMagicOnObjectInteraction) -> Boolean) {
        require(objectIds.isNotEmpty() && spellIds.isNotEmpty())
        magicObjects += QuestMagicOnObjectBinding(objectIds.distinct().toIntArray(), spellIds.distinct().toIntArray(), handler)
    }

    fun button(requiredInterfaceId: Int = -1, opIndex: Int? = null, vararg rawButtonIds: Int, handler: (QuestButtonInteraction) -> Boolean) {
        require(rawButtonIds.isNotEmpty() && requiredInterfaceId >= -1)
        buttons += QuestButtonBinding(rawButtonIds.distinct().toIntArray(), requiredInterfaceId, opIndex, handler)
    }

    internal fun build() = QuestPluginDefinition(name, objects, npcs, pairs, items, itemObjects, magicObjects, buttons)
}

fun questPlugin(name: String, block: QuestPluginBuilder.() -> Unit): QuestPluginDefinition =
    QuestPluginBuilder(name).apply(block).build()
