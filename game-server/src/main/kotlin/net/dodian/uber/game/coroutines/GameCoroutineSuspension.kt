package net.dodian.uber.game.coroutines

import net.dodian.uber.game.coroutines.resume.ResumeCondition
import kotlin.coroutines.Continuation

data class GameCoroutineSuspension<T>(
    val continuation: Continuation<T>,
    val condition: ResumeCondition<T>
)
