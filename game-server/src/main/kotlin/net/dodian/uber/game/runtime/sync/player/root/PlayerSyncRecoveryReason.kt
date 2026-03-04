package net.dodian.uber.game.runtime.sync.player.root

enum class PlayerSyncRecoveryReason {
    CURRENT_LOCAL_MISMATCH,
    LOCAL_COUNT_OUT_OF_RANGE,
    DUPLICATE_LOCAL,
    PENDING_ALREADY_LOCAL,
    REGION_STATE_MISMATCH,
    STALE_LOCAL,
}
