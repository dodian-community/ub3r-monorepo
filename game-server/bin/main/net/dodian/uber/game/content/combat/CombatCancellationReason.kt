package net.dodian.uber.game.content.combat

enum class CombatCancellationReason {
    DISCONNECTED,
    LOGOUT,
    DEATH,
    TARGET_INVALID,
    OUT_OF_RANGE,
    MOVEMENT_INTERRUPTED,
    INTERACTION_PREEMPTED,
}
