package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

typealias NpcClickHandler = (Client, Npc) -> Boolean
internal val NO_CLICK_HANDLER: NpcClickHandler = { _, _ -> false }

fun npcIds(vararg ids: Int): IntArray = ids.distinct().toIntArray()

fun npcIdsFromEntries(entries: List<NpcSpawnDef>): IntArray = entries
    .map { it.npcId }
    .distinct()
    .toIntArray()

fun npcIdsFromEntries(entries: List<NpcSpawnDef>, vararg additionalNpcIds: Int): IntArray = buildList {
    addAll(entries.map { it.npcId })
    addAll(additionalNpcIds.toList())
}
    .distinct()
    .toIntArray()

data class NpcContentDefinition(
    val name: String,
    val npcIds: IntArray,
    val ownsSpawnDefinitions: Boolean = false,
    val entries: List<NpcSpawnDef> = emptyList(),
    val onFirstClick: NpcClickHandler = NO_CLICK_HANDLER,
    val onSecondClick: NpcClickHandler = NO_CLICK_HANDLER,
    val onThirdClick: NpcClickHandler = NO_CLICK_HANDLER,
    val onFourthClick: NpcClickHandler = NO_CLICK_HANDLER,
    val onAttack: NpcClickHandler = NO_CLICK_HANDLER,
)
