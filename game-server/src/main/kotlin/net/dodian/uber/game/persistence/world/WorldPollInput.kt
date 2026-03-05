package net.dodian.uber.game.persistence.world

data class WorldPollInput(
    val worldId: Int,
    val playerCount: Int,
    val onlinePlayerDbIds: List<Int> = emptyList(),
) {
    init {
        // Keep caller-visible list immutable.
        @Suppress("unused")
        onlinePlayerDbIds.toList()
    }
}
