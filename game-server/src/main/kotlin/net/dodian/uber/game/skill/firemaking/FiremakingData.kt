package net.dodian.uber.game.skill.firemaking

data class FiremakingLog(
    val itemId: Int,
    val level: Int,
    val xp: Int,
    val name: String,
)

object FiremakingData {
    private val logs = listOf(
        FiremakingLog(1511, 1, 160, "logs"),
        FiremakingLog(1521, 15, 240, "oak logs"),
        FiremakingLog(1519, 30, 360, "willow logs"),
        FiremakingLog(1517, 45, 540, "maple logs"),
        FiremakingLog(1515, 60, 812, "yew logs"),
        FiremakingLog(1513, 75, 1216, "magic logs"),
    )

    @JvmStatic
    fun findLog(itemUsed: Int, useWith: Int): FiremakingLog? =
        logs.firstOrNull { itemUsed == it.itemId || useWith == it.itemId }
}
