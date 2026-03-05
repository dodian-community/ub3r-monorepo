package net.dodian.uber.game.runtime.tasking.suspension

import kotlin.coroutines.Continuation

data class TaskStep(
    val condition: TaskCondition,
    val continuation: Continuation<Unit>,
)
