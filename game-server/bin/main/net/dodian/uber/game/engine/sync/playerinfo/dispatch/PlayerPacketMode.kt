package net.dodian.uber.game.engine.sync.playerinfo.dispatch

enum class PlayerPacketMode {
    SKIP,
    SELF_ONLY,
    INCREMENTAL_STEADY,
    INCREMENTAL_ADMISSION,
    FULL_REBUILD,
}
