package net.dodian.uber.game.content.npcs.yanille

import net.dodian.uber.game.content.npcs.NpcContent
import net.dodian.uber.game.content.npcs.NpcDispatcher
import net.dodian.uber.game.content.npcs.NpcSpawnDef
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

object GuardNpc : NpcContent {
    override val npcIds: IntArray = intArrayOf(3094)

    override fun onAttack(client: Client, npc: Npc, npcIndex: Int): Boolean {
        NpcDispatcher.attackLikeLegacy(client, npc, npcIndex)
        return true
    }

    override fun spawns(): List<NpcSpawnDef> = listOf(
        NpcSpawnDef(
            npcId = 3094,
            x = 2606,
            y = 3102,
            z = 0,
            face = 0,
            respawnTicks = -1,
            attack = 999,
            defence = -1,
            strength = 999,
            hitpoints = 1000,
            ranged = -1,
            magic = -1,
        )
    )
}
