package net.dodian.uber.game.content.npcs

@Deprecated("Use net.dodian.uber.game.npc.NpcClickHandler")
typealias NpcClickHandler = net.dodian.uber.game.npc.NpcClickHandler

@Deprecated("Use net.dodian.uber.game.npc.NO_CLICK_HANDLER")
internal val NO_CLICK_HANDLER: NpcClickHandler = net.dodian.uber.game.npc.NO_CLICK_HANDLER

@Deprecated("Use net.dodian.uber.game.npc.NpcInteractionSource")
typealias NpcInteractionSource = net.dodian.uber.game.npc.NpcInteractionSource

@Deprecated("Use net.dodian.uber.game.npc.NpcModule")
typealias NpcModule = net.dodian.uber.game.npc.NpcModule

@Deprecated("Use net.dodian.uber.game.npc.npcIds")
fun npcIds(vararg ids: Int): IntArray = net.dodian.uber.game.npc.npcIds(*ids)

@Deprecated("Use net.dodian.uber.game.npc.npcIdsFromEntries")
fun npcIdsFromEntries(entries: List<NpcSpawnDef>): IntArray = net.dodian.uber.game.npc.npcIdsFromEntries(entries)

@Deprecated("Use net.dodian.uber.game.npc.npcIdsFromEntries")
fun npcIdsFromEntries(entries: List<NpcSpawnDef>, vararg additionalNpcIds: Int): IntArray =
    net.dodian.uber.game.npc.npcIdsFromEntries(entries, *additionalNpcIds)

@Deprecated("Use net.dodian.uber.game.npc.NpcContentDefinition")
typealias NpcContentDefinition = net.dodian.uber.game.npc.NpcContentDefinition

@Deprecated("Use net.dodian.uber.game.npc.optionLabel")
fun NpcContentDefinition.optionLabel(option: Int): String? = optionLabels[option]

@Deprecated("Use net.dodian.uber.game.npc.hasInteractionHandlers")
internal fun NpcContentDefinition.hasInteractionHandlers(): Boolean {
    return onFirstClick !== NO_CLICK_HANDLER ||
        onSecondClick !== NO_CLICK_HANDLER ||
        onThirdClick !== NO_CLICK_HANDLER ||
        onFourthClick !== NO_CLICK_HANDLER ||
        onAttack !== NO_CLICK_HANDLER
}
