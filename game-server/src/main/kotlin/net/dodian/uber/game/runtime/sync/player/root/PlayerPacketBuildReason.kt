package net.dodian.uber.game.runtime.sync.player.root

enum class PlayerPacketBuildReason {
    INITIAL_SYNC,
    SELF_MOVEMENT,
    SELF_BLOCK,
    RETAINED_LOCAL_CHANGED,
    LOCAL_REMOVAL,
    LOCAL_ADMISSION_PENDING,
    TELEPORT,
    MAP_REGION_CHANGE,
    BUILD_AREA_CHANGE,
    STATE_MISMATCH_RECOVERY,
}
