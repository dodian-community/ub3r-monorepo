package net.dodian.uber.game.persistence.player

enum class PlayerSaveReason {
    PERIODIC,
    PERIODIC_PROGRESS,
    TRADE,
    DUEL,
    LOGOUT,
    DISCONNECT,
    SHUTDOWN,
}
