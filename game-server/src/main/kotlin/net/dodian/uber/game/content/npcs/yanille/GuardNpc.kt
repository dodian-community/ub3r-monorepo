package net.dodian.uber.game.content.npcs.yanille

import net.dodian.uber.game.content.npcs.NpcContent
import net.dodian.uber.game.content.npcs.NpcDataPreset
import net.dodian.uber.game.content.npcs.NpcDispatcher
import net.dodian.uber.game.content.npcs.NpcSpawnDef
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

object GuardNpc : NpcContent {
    private const val GUARD_NPC_ID = 3094
    private const val BASIC_GUARD_PRESET_SLOT = 0
    private const val ELITE_GUARD_PRESET_SLOT = 1

    override val npcIds: IntArray = intArrayOf(GUARD_NPC_ID)

    override fun onAttack(client: Client, npc: Npc, npcIndex: Int): Boolean {
        NpcDispatcher.attackLikeLegacy(client, npc, npcIndex)
        return true
    }

    // Shared stat presets: useful when multiple spawns use the same data.
    override fun npcDataPresets(): List<NpcDataPreset> = listOf(
        NpcDataPreset(
            respawnTicks = -1,
            attack = 999,
            defence = -1,
            strength = 999,
            hitpoints = 1000,
            ranged = -1,
            magic = -1,
        ),
        NpcDataPreset(
            respawnTicks = 25,
            attack = 1200,
            defence = 1200,
            strength = 1200,
            hitpoints = 1500,
            ranged = -1,
            magic = -1,
        )
    )

    // World placements: use npcDataSlot for shared preset values.
    override fun spawns(): List<NpcSpawnDef> = listOf(
        spawn(x = 2606, y = 3102, face = 0, npcDataSlot = BASIC_GUARD_PRESET_SLOT),
        spawn(x = 2607, y = 3102, face = 1, npcDataSlot = BASIC_GUARD_PRESET_SLOT, attack = 1100),
        spawn(x = 2608, y = 3102, face = 2, npcDataSlot = ELITE_GUARD_PRESET_SLOT),
        spawn(x = 2609, y = 3102, face = 3, respawnTicks = 15, attack = 900, defence = 900, strength = 900, hitpoints = 1200),
    )

    private fun spawn(
        x: Int,
        y: Int,
        face: Int,
        npcDataSlot: Int = -1,
        respawnTicks: Int = -1,
        attack: Int = -1,
        defence: Int = -1,
        strength: Int = -1,
        hitpoints: Int = -1,
        ranged: Int = -1,
        magic: Int = -1,
    ): NpcSpawnDef {
        return NpcSpawnDef(
            npcId = GUARD_NPC_ID,
            npcDataSlot = npcDataSlot,
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
