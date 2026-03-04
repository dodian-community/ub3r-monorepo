package net.dodian.uber.game.runtime.loop

enum class GamePhase {
    INBOUND_PACKETS,
    WORLD_DB_INPUT_BUILD,
    WORLD_DB_RESULT_READ,
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
