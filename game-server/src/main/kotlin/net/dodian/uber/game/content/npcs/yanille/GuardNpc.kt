package net.dodian.uber.game.content.npcs.yanille

import net.dodian.uber.game.content.npcs.NpcContent
import net.dodian.uber.game.content.npcs.NpcDispatcher
import net.dodian.uber.game.content.npcs.NpcSpawnDef
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

object GuardNpc : NpcContent {
    private const val GUARD_NPC_ID = 3094

    override val npcIds: IntArray = intArrayOf(GUARD_NPC_ID)

    override fun onAttack(client: Client, npc: Npc, npcIndex: Int): Boolean {
        NpcDispatcher.attackLikeLegacy(client, npc, npcIndex)
        return true
    }

    // World placements with per-spawn stat overrides.
    override fun spawns(): List<NpcSpawnDef> = listOf(
        spawn(npcId = GUARD_NPC_ID, x = 2606, y = 3102, face = 0),
        spawn(npcId = GUARD_NPC_ID, x = 2607, y = 3102, face = 1, attack = 1100),
        spawn(npcId = GUARD_NPC_ID, x = 2608, y = 3102, face = 2),
        spawn(npcId = GUARD_NPC_ID, x = 2609, y = 3102, face = 3, respawnTicks = 15, attack = 900, defence = 900, strength = 900, hitpoints = 1200),
    )

    private fun spawn(
        npcId: Int,
        x: Int,
        y: Int,
        face: Int,
        respawnTicks: Int = -1,
        attack: Int = -1,
        defence: Int = -1,
        strength: Int = -1,
        hitpoints: Int = -1,
        ranged: Int = -1,
        magic: Int = -1,
    ): NpcSpawnDef {
        return NpcSpawnDef(
            npcId = npcId,
            x = x,
            y = y,
            z = 0,
            face = face,
            respawnTicks = respawnTicks,
            attack = attack,
            defence = defence,
            strength = strength,
            hitpoints = hitpoints,
            ranged = ranged,
            magic = magic
        )
    }
}
