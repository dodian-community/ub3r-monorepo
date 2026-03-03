package net.dodian.uber.game.runtime.world.farming

data class FarmingTickBucket(
    val cycle: Long,
    val duePlayers: DuePlayerSet = DuePlayerSet(),
)
