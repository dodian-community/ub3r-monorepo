package net.dodian.uber.game.runtime.loop

enum class GamePhase {
    INBOUND_PACKETS,
    WORLD_DB_POLL,
    WORLD_DB_APPLY,
    FARMING_TICK,
    PLUNDER_DOOR,
    NPC_MAIN,
    PLAYER_MAIN,
    LEGACY_ACTIONS,
    MOVEMENT_FINALIZE,
    OUTBOUND_SYNC,
    HOUSEKEEPING,
}
