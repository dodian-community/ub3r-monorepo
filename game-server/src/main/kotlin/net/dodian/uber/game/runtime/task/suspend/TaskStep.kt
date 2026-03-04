package net.dodian.uber.game.runtime.task.suspension

import kotlin.coroutines.Continuation

data class TaskStep(
    val condition: TaskCondition,
    val continuation: Continuation<Unit>,
)
