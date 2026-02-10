package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.model.entity.player.Client

data class NpcSpawnDef(
    val npcId: Int,
    val x: Int,
    val y: Int,
    val z: Int = 0,
    val face: Int = 0,
    val walkRadius: Int = 0,
    val attackRange: Int = 6,
    val alwaysActive: Boolean = false,
    val condition: (Client) -> Boolean = { true },
    val preset: NpcDataPreset? = null,
    val respawnTicks: Int = -1,
    val attack: Int = -1,
    val defence: Int = -1,
    val strength: Int = -1,
    val hitpoints: Int = -1,
    val ranged: Int = -1,
    val magic: Int = -1,
)
