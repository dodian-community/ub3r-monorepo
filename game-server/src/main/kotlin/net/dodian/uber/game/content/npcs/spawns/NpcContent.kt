package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

typealias NpcClickHandler = (Client, Npc) -> Boolean

data class NpcContentDefinition(
    val name: String,
    val npcIds: IntArray,
    val ownsSpawnDefinitions: Boolean = false,
    val entries: List<NpcSpawnDef> = emptyList(),
    val onFirstClick: NpcClickHandler = { _, _ -> false },
    val onSecondClick: NpcClickHandler = { _, _ -> false },
    val onThirdClick: NpcClickHandler = { _, _ -> false },
    val onFourthClick: NpcClickHandler = { _, _ -> false },
    val onAttack: NpcClickHandler = { _, _ -> false },
)
