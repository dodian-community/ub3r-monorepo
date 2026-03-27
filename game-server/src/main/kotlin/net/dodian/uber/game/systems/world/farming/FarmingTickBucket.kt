package net.dodian.uber.game.systems.world.farming

data class FarmingTickBucket(
    val cycle: Long,
    val duePlayers: DuePlayerSet = DuePlayerSet(),
)
