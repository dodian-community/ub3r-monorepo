package net.dodian.uber.game.runtime.sync.player

data class PlayerSyncFastPathStats(
    val packetsBuilt: Int,
    val packetsSkipped: Int,
    val packetsTemplated: Int,
)
