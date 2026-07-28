package net.dodian.uber.game.engine.tasking

/** Scheduling intent declared by content; the server maps it onto its task queue. */
enum class TaskPriority {
    WEAK,
    STANDARD,
    STRONG,
}
