package net.dodian.uber.game.content.skills.runtime.action

enum class ActionStopReason {
    USER_INTERRUPT,
    BUSY,
    FULL_INVENTORY,
    MISSING_TOOL,
    MOVED_AWAY,
    DISCONNECTED,
    INVALID_TARGET,
    REQUIREMENT_FAILED,
    COMPLETED,
}
