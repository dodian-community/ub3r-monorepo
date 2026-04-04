package net.dodian.uber.game.systems.skills

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.systems.api.content.ContentObjectInteractionPolicy

interface SkillPlugin {
    val definition: SkillPluginDefinition
}

data class SkillPluginDefinition(
    val name: String,
    val skill: Skill,
    val objectBindings: List<SkillObjectClickBinding> = emptyList(),
    val npcBindings: List<SkillNpcClickBinding> = emptyList(),
    val itemOnItemBindings: List<SkillItemOnItemBinding> = emptyList(),
    val buttonBindings: List<SkillButtonBinding> = emptyList(),
    val lifecycle: SkillPluginLifecycleHooks = SkillPluginLifecycleHooks(),
)

data class SkillPluginLifecycleHooks(
    val onAttempt: ((Client) -> Unit)? = null,
    val onStart: ((Client) -> Unit)? = null,
    val onCycle: ((Client) -> Unit)? = null,
    val onStop: ((Client) -> Unit)? = null,
)

data class SkillObjectClickBinding(
    val option: Int,
    val objectIds: IntArray,
    val handler: (client: Client, objectId: Int, position: Position, obj: GameObjectData?) -> Boolean,
    val policyResolver: (
        (
            option: Int,
            objectId: Int,
            position: Position,
            obj: GameObjectData?,
        ) -> ContentObjectInteractionPolicy?
    )? = null,
)

data class SkillNpcClickBinding(
    val option: Int,
    val npcIds: IntArray,
    val handler: (client: Client, npc: Npc) -> Boolean,
)

data class SkillItemOnItemBinding(
    val leftItemId: Int,
    val rightItemId: Int,
    val handler: (client: Client, itemUsed: Int, otherItem: Int) -> Boolean,
)

data class SkillButtonBinding(
    val rawButtonIds: IntArray,
    val opIndex: Int? = null,
    val handler: (client: Client, rawButtonId: Int, opIndex: Int) -> Boolean,
)
