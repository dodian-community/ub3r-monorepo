package net.dodian.uber.game.content.npcs

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

interface NpcContent {
    val npcIds: IntArray
    fun spawns(): List<NpcSpawnDef> = emptyList()

    fun onFirstClick(client: Client, npc: Npc, npcIndex: Int): Boolean = false
    fun onSecondClick(client: Client, npc: Npc, npcIndex: Int): Boolean = false
    fun onThirdClick(client: Client, npc: Npc, npcIndex: Int): Boolean = false
    fun onFourthClick(client: Client, npc: Npc, npcIndex: Int): Boolean = false
    fun onFifthClick(client: Client, npc: Npc, npcIndex: Int): Boolean = false

    fun onAttack(client: Client, npc: Npc, npcIndex: Int): Boolean = false
}
