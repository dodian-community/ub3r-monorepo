package net.dodian.uber.game.runtime.world

enum class WorldMaintenanceStage {
    WORLD_DB_POLL,
    WORLD_DB_APPLY,
    FARMING_TICK,
    PLUNDER_DOOR,
}
