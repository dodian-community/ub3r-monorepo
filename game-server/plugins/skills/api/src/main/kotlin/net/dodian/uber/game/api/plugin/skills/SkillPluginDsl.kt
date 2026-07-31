package net.dodian.uber.game.api.plugin.skills

import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.model.player.skills.Skill

class SkillPluginBuilder internal constructor(private val name: String, private val skill: Skill) {
    private val objects = mutableListOf<SkillObjectClickBinding>(); private val npcs = mutableListOf<SkillNpcClickBinding>()
    private val pairs = mutableListOf<SkillItemOnItemBinding>(); private val items = mutableListOf<SkillItemClickBinding>()
    private val itemObjects = mutableListOf<SkillItemOnObjectBinding>(); private val magicObjects = mutableListOf<SkillMagicOnObjectBinding>()
    private val buttons = mutableListOf<SkillButtonBinding>(); private val itemGrids = mutableListOf<SkillItemGridBinding>(); private var hooks = SkillPluginLifecycleHooks()
    private val confirmDialogues = mutableListOf<SkillConfirmDialogueBinding>()
    private val itemOnNpcs = mutableListOf<SkillItemOnNpcBinding>(); private val playerOptionMenus = mutableListOf<SkillPlayerOptionMenuBinding>(); private val magicOnItems = mutableListOf<SkillMagicOnItemBinding>()
    fun objectClick(preset: PolicyPreset, option: Int = 1, vararg objectIds: Int, handler: (SkillObjectInteraction) -> Boolean) { require(option in 1..5 && objectIds.isNotEmpty()); objects += SkillObjectClickBinding(preset, option, objectIds.distinct().toIntArray(), handler) }
    fun npcClick(preset: PolicyPreset, option: Int, vararg npcIds: Int, handler: (SkillNpcInteraction) -> Boolean) { require(option in 1..4 && npcIds.isNotEmpty()); npcs += SkillNpcClickBinding(preset, option, npcIds.distinct().toIntArray(), handler) }
    fun itemOnItem(preset: PolicyPreset, leftItemId: Int, rightItemId: Int, handler: (SkillItemOnItemInteraction) -> Boolean) { require(leftItemId >= 0 && rightItemId >= 0); pairs += SkillItemOnItemBinding(preset, leftItemId, rightItemId, handler) }
    fun itemClick(preset: PolicyPreset, option: Int, vararg itemIds: Int, handler: (SkillItemInteraction) -> Boolean) { require(option in 1..3 && itemIds.isNotEmpty()); items += SkillItemClickBinding(preset, option, itemIds.distinct().toIntArray(), handler) }
    fun itemOnObject(preset: PolicyPreset, vararg objectIds: Int, itemIds: IntArray = intArrayOf(-1), handler: (SkillItemOnObjectInteraction) -> Boolean) { require(objectIds.isNotEmpty() && itemIds.isNotEmpty()); itemObjects += SkillItemOnObjectBinding(preset, objectIds.distinct().toIntArray(), itemIds.distinct().toIntArray(), handler) }
    fun magicOnObject(preset: PolicyPreset, vararg objectIds: Int, spellIds: IntArray = intArrayOf(-1), handler: (SkillMagicOnObjectInteraction) -> Boolean) { require(objectIds.isNotEmpty() && spellIds.isNotEmpty()); magicObjects += SkillMagicOnObjectBinding(preset, objectIds.distinct().toIntArray(), spellIds.distinct().toIntArray(), handler) }
    fun button(preset: PolicyPreset, requiredInterfaceId: Int = -1, opIndex: Int? = null, vararg rawButtonIds: Int, handler: (SkillButtonInteraction) -> Boolean) { require(rawButtonIds.isNotEmpty() && requiredInterfaceId >= -1); buttons += SkillButtonBinding(preset, rawButtonIds.distinct().toIntArray(), requiredInterfaceId, opIndex, handler) }
    fun itemGrid(preset: PolicyPreset, interfaceId: Int, handler: (SkillItemGridInteraction) -> Boolean) { require(interfaceId > 0); itemGrids += SkillItemGridBinding(preset, interfaceId, handler) }
    fun confirmDialogue(preset: PolicyPreset, dialogueId: Int, message: String, yesLabel: String = "Yes", noLabel: String = "No", handler: (SkillPlayer) -> Unit) { require(dialogueId > 0); confirmDialogues += SkillConfirmDialogueBinding(preset, dialogueId, message, yesLabel, noLabel, handler) }
    fun itemOnNpc(preset: PolicyPreset, vararg npcIds: Int, itemIds: IntArray = intArrayOf(-1), handler: (SkillItemOnNpcInteraction) -> Boolean) { require(npcIds.isNotEmpty() && itemIds.isNotEmpty()); itemOnNpcs += SkillItemOnNpcBinding(preset, npcIds.distinct().toIntArray(), itemIds.distinct().toIntArray(), handler) }
    fun playerOptionMenu(preset: PolicyPreset, dialogueId: Int, title: String, options: List<String>, guard: ((SkillPlayer) -> String?)? = null, handler: (SkillPlayer, Int) -> Unit) { require(dialogueId > 0 && options.size in 2..5); playerOptionMenus += SkillPlayerOptionMenuBinding(preset, dialogueId, title, options, guard, handler) }
    fun magicOnItem(preset: PolicyPreset, vararg itemIds: Int, spellIds: IntArray = intArrayOf(-1), handler: (SkillMagicOnItemInteraction) -> Boolean) { require(itemIds.isNotEmpty() && spellIds.isNotEmpty()); magicOnItems += SkillMagicOnItemBinding(preset, itemIds.distinct().toIntArray(), spellIds.distinct().toIntArray(), handler) }
    fun lifecycle(block: SkillPluginLifecycleBuilder.() -> Unit) { hooks = SkillPluginLifecycleBuilder().apply(block).build() }
    fun startSession(key: String) = merge(onStart = { it.actions.beginSession(key) })
    fun requireSession(key: String) = merge(onAttempt = { if (it.actions.activeSessionKey()?.let { active -> active != key } == true) it.ui.message("You are already doing another skill action.") })
    fun endSession(key: String) = merge(onStop = { it.actions.endSession(key) })
    private fun merge(onAttempt: ((SkillPlayer)->Unit)?=null,onStart:((SkillPlayer)->Unit)?=null,onStop:((SkillPlayer)->Unit)?=null) { hooks = hooks.copy(onAttempt=compose(hooks.onAttempt,onAttempt),onStart=compose(hooks.onStart,onStart),onStop=compose(hooks.onStop,onStop)) }
    private fun compose(a:((SkillPlayer)->Unit)?,b:((SkillPlayer)->Unit)?):((SkillPlayer)->Unit)? = when { a==null->b; b==null->a; else->{ p -> a(p); b(p) } }
    internal fun build() = SkillPluginDefinition(name, skill, objects, npcs, pairs, items, itemObjects, magicObjects, buttons, itemGrids, confirmDialogues, itemOnNpcs, playerOptionMenus, magicOnItems, hooks)
}
class SkillPluginLifecycleBuilder internal constructor() {
    private var attempt:((SkillPlayer)->Unit)?=null; private var start:((SkillPlayer)->Unit)?=null; private var cycle:((SkillPlayer)->Unit)?=null; private var stop:((SkillPlayer)->Unit)?=null
    fun onAttempt(handler:(SkillPlayer)->Unit){attempt=handler}; fun onStart(handler:(SkillPlayer)->Unit){start=handler}; fun onCycle(handler:(SkillPlayer)->Unit){cycle=handler}; fun onStop(handler:(SkillPlayer)->Unit){stop=handler}
    internal fun build()=SkillPluginLifecycleHooks(attempt,start,cycle,stop)
}
fun skillPlugin(name:String,skill:Skill,block:SkillPluginBuilder.()->Unit)=SkillPluginBuilder(name,skill).apply(block).build()
