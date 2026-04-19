package net.dodian.uber.game.persistence

import net.dodian.uber.game.persistence.world.WorldPollInput

/** Immutable snapshot of world state captured each maintenance cycle and passed to the DB save pipeline. */
data class WorldSaveSnapshot(
    val worldId: Int,
    val playerCount: Int,
    val onlinePlayerDbIds: IntArray,
) {
    fun toInput(): WorldPollInput = WorldPollInput(worldId, playerCount, onlinePlayerDbIds.asList())
}
