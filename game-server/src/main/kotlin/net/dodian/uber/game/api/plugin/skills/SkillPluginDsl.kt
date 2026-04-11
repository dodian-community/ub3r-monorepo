package net.dodian.uber.game.api.plugin.skills

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.engine.systems.action.PolicyPreset

class SkillPluginBuilder internal constructor(
    private val name: String,
    private val skill: Skill,
) {
    private val objectBindings = ArrayList<SkillObjectClickBinding>()
    private val npcBindings = ArrayList<SkillNpcClickBinding>()
    private val itemOnItemBindings = ArrayList<SkillItemOnItemBinding>()
    private val itemBindings = ArrayList<SkillItemClickBinding>()
    private val itemOnObjectBindings = ArrayList<SkillItemOnObjectBinding>()
    private val buttonBindings = ArrayList<SkillButtonBinding>()
    private var lifecycle = SkillPluginLifecycleHooks()

    fun objectClick(
        preset: PolicyPreset,
        option: Int = 1,
        vararg objectIds: Int,
        handler: (client: Client, objectId: Int, position: Position, obj: GameObjectData?) -> Boolean,
    ) {
        require(option in 1..5) { "Object option must be between 1 and 5." }
        require(objectIds.isNotEmpty()) { "Object click binding requires at least one object id." }
        objectBindings += SkillObjectClickBinding(preset, option, objectIds.toSet().toIntArray(), handler)
    }

    fun npcClick(
        preset: PolicyPreset,
        option: Int,
        vararg npcIds: Int,
        handler: (client: Client, npc: Npc) -> Boolean,
    ) {
        require(option in 1..4) { "NPC option must be between 1 and 4." }
        require(npcIds.isNotEmpty()) { "Npc click binding requires at least one npc id." }
        npcBindings += SkillNpcClickBinding(preset, option, npcIds.toSet().toIntArray(), handler)
    }

    fun itemOnItem(
        preset: PolicyPreset,
        leftItemId: Int,
        rightItemId: Int,
        handler: (client: Client, itemUsed: Int, otherItem: Int) -> Boolean,
    ) {
        require(leftItemId >= 0 && rightItemId >= 0) { "Item ids must be non-negative." }
        itemOnItemBindings += SkillItemOnItemBinding(preset, leftItemId, rightItemId, handler)
    }

    fun itemClick(
        preset: PolicyPreset,
        option: Int,
        vararg itemIds: Int,
        handler: (client: Client, itemId: Int, itemSlot: Int, interfaceId: Int) -> Boolean,
    ) {
        require(option in 1..3) { "Item option must be between 1 and 3." }
        require(itemIds.isNotEmpty()) { "Item click binding requires at least one item id." }
        itemBindings += SkillItemClickBinding(preset, option, itemIds.toSet().toIntArray(), handler)
    }

    fun itemOnObject(
        preset: PolicyPreset,
        vararg objectIds: Int,
        itemIds: IntArray = intArrayOf(-1),
        handler: (
            client: Client,
            objectId: Int,
            position: Position,
            obj: GameObjectData?,
            itemId: Int,
            itemSlot: Int,
            interfaceId: Int,
        ) -> Boolean,
    ) {
        require(objectIds.isNotEmpty()) { "Item-on-object binding requires at least one object id." }
        require(itemIds.isNotEmpty()) { "Item-on-object binding requires at least one item id or wildcard (-1)." }
        itemOnObjectBindings += SkillItemOnObjectBinding(
            preset = preset,
            objectIds = objectIds.toSet().toIntArray(),
            itemIds = itemIds.toSet().toIntArray(),
            handler = handler,
        )
    }

    fun button(
        preset: PolicyPreset,
        vararg rawButtonIds: Int,
        opIndex: Int? = null,
        handler: (client: Client, rawButtonId: Int, op: Int) -> Boolean,
    ) {
        require(rawButtonIds.isNotEmpty()) { "Button binding requires at least one raw button id." }
        buttonBindings += SkillButtonBinding(preset, rawButtonIds.toSet().toIntArray(), opIndex, handler)
    }

    fun lifecycle(block: SkillPluginLifecycleBuilder.() -> Unit) {
        lifecycle = SkillPluginLifecycleBuilder().apply(block).build()
    }

    internal fun build(): SkillPluginDefinition {
        return SkillPluginDefinition(
            name = name,
            skill = skill,
            objectBindings = objectBindings.toList(),
            npcBindings = npcBindings.toList(),
            itemOnItemBindings = itemOnItemBindings.toList(),
            itemBindings = itemBindings.toList(),
            itemOnObjectBindings = itemOnObjectBindings.toList(),
            buttonBindings = buttonBindings.toList(),
            lifecycle = lifecycle,
        )
    }
}

class SkillPluginLifecycleBuilder internal constructor() {
    private var onAttempt: ((Client) -> Unit)? = null
    private var onStart: ((Client) -> Unit)? = null
    private var onCycle: ((Client) -> Unit)? = null
    private var onStop: ((Client) -> Unit)? = null

    fun onAttempt(handler: (Client) -> Unit) {
        onAttempt = handler
    }

    fun onStart(handler: (Client) -> Unit) {
        onStart = handler
    }

    fun onCycle(handler: (Client) -> Unit) {
        onCycle = handler
    }

    fun onStop(handler: (Client) -> Unit) {
        onStop = handler
    }

    internal fun build(): SkillPluginLifecycleHooks {
        return SkillPluginLifecycleHooks(onAttempt, onStart, onCycle, onStop)
    }
}

fun skillPlugin(
    name: String,
    skill: Skill,
    block: SkillPluginBuilder.() -> Unit,
): SkillPluginDefinition {
    return SkillPluginBuilder(name, skill).apply(block).build()
}
