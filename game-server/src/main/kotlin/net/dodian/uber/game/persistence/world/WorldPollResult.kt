package net.dodian.uber.game.persistence.world

data class WorldPollResult(
    val latestNewsId: Int?,
    val playersWithRefunds: Set<Int>,
    val muteTimes: Map<Int, Long>,
    val bannedPlayerIds: Set<Int>,
) {
    companion object {
        @JvmField
        val EMPTY = WorldPollResult(null, emptySet(), emptyMap(), emptySet())
    }
}
