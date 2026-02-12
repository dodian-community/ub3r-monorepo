package net.dodian.uber.game.content.npcs.click

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

interface NpcClickContent {
    val npcIds: IntArray

    fun onFirstClick(client: Client, npc: Npc): Boolean = false
    fun onSecondClick(client: Client, npc: Npc): Boolean = false
    fun onThirdClick(client: Client, npc: Npc): Boolean = false
    fun onFourthClick(client: Client, npc: Npc): Boolean = false
}

