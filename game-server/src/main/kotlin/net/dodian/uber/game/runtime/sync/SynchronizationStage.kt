package net.dodian.uber.game.runtime.sync

enum class SynchronizationStage {
    SYNC_PLAYER_PREP,
    SYNC_PLAYER_ENCODE,
    SYNC_NPC_PREP,
    SYNC_NPC_ENCODE,
    SYNC_FLUSH,
    SYNC_FLAG_CLEAR,
    SYNC_METRICS_LOG,
}
