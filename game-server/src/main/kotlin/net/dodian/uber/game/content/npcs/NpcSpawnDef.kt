package net.dodian.uber.game.content.npcs

data class NpcSpawnDef(
    val npcId: Int,
    val npcDataSlot: Int = -1,
    val x: Int,
    val y: Int,
    val z: Int = 0,
    val face: Int = 0,
    val respawnTicks: Int = -1,
    val attack: Int = -1,
    val defence: Int = -1,
    val strength: Int = -1,
    val hitpoints: Int = -1,
    val ranged: Int = -1,
    val magic: Int = -1,
)
