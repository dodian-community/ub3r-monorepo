package net.dodian.uber.game.content.npcs

data class NpcDataPreset(
    val respawnTicks: Int = -1,
    val attack: Int = -1,
    val defence: Int = -1,
    val strength: Int = -1,
    val hitpoints: Int = -1,
    val ranged: Int = -1,
    val magic: Int = -1,
)
