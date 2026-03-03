package net.dodian.uber.game.runtime.loop

enum class GamePhase {
    INBOUND_PACKETS,
    WORLD_MAINTENANCE,
    NPC_MAIN,
    PLAYER_MAIN,
    LEGACY_ACTIONS,
    MOVEMENT_FINALIZE,
    OUTBOUND_SYNC,
    HOUSEKEEPING,
}
