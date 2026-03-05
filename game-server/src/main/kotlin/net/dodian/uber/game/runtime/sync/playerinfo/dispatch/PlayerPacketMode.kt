package net.dodian.uber.game.runtime.sync.playerinfo.dispatch

enum class PlayerPacketMode {
    SKIP,
    SELF_ONLY,
    INCREMENTAL_STEADY,
    INCREMENTAL_ADMISSION,
    FULL_REBUILD,
}
