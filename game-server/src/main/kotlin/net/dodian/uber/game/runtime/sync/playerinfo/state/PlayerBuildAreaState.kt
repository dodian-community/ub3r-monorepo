package net.dodian.uber.game.runtime.sync.playerinfo.state

data class PlayerBuildAreaState(
    var regionBaseX: Int = Int.MIN_VALUE,
    var regionBaseY: Int = Int.MIN_VALUE,
    var plane: Int = Int.MIN_VALUE,
    var visibleSignature: Int = 0,
    var forcedRebuild: Boolean = true,
)
