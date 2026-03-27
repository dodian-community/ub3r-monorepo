package net.dodian.uber.game.persistence

import net.dodian.uber.game.persistence.world.WorldPollInput
data class WorldPollSnapshot(
    val worldId: Int,
    val playerCount: Int,
    val onlinePlayerDbIds: IntArray,
) {
    fun toInput(): WorldPollInput = WorldPollInput(worldId, playerCount, onlinePlayerDbIds.asList())
}
