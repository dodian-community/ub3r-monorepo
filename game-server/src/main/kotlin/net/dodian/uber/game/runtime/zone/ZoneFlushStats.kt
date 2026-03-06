package net.dodian.uber.game.runtime.zone

data class ZoneFlushStats(
    val deltas: Int,
    val candidateViewers: Int,
    val deliveries: Int,
) {
    companion object {
        @JvmField
        val EMPTY = ZoneFlushStats(0, 0, 0)
    }
}
