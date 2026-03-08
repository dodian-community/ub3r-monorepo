package net.dodian.uber.game.runtime.tasking

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import net.dodian.uber.game.runtime.loop.GameCycleClock
import net.dodian.uber.game.runtime.tasking.suspension.PredicateCondition
import net.dodian.uber.game.runtime.tasking.suspension.TaskStep
import net.dodian.uber.game.runtime.tasking.suspension.WaitCondition
import org.slf4j.LoggerFactory

class StopControlScope internal constructor(
    private val task: GameTask,
) {
    private var stopped = false

    fun stop() {
        stopped = true
    }

    internal fun isStopped(): Boolean = stopped

    suspend fun wait(cycles: Int) {
        task.wait(cycles)
    }
}

class GameTask internal constructor(
    val ctx: Any,
    val priority: TaskPriority,
    private val control: TaskControl,
) : Continuation<Unit> {
    internal lateinit var coroutine: Continuation<Unit>
    internal var invoked = false
    private var nextStep: TaskStep? = null
    private val returnValues = HashMap<TaskRequestKey<*>, Any?>()
    private var terminateAction: (GameTask.() -> Unit)? = null

    override val context: CoroutineContext = EmptyCoroutineContext

    override fun resumeWith(result: Result<Unit>) {
        nextStep = null
        control.markCompleted()
        result.exceptionOrNull()?.let { exception ->
            logger.error("Game task failed", exception)
        }
    }

    internal fun cycle() {
        val step = nextStep ?: return
        if (step.condition.resume()) {
            nextStep = null
            step.continuation.resume(Unit)
        }
    }

    internal fun submitReturnValue(key: TaskRequestKey<*>, value: Any?) {
        returnValues[key] = value
    }

    internal fun isFinished(): Boolean = control.completed || control.cancelled

    fun terminate() {
        if (control.completed) {
            return
        }
        nextStep = null
        returnValues.clear()
        terminateAction?.invoke(this)
        control.markCompleted()
    }

    fun onTerminate(block: GameTask.() -> Unit) {
        terminateAction = block
    }

    suspend fun wait(ticks: Int): Unit =
        suspendCoroutine { continuation ->
            require(ticks > 0) { "Wait ticks must be greater than 0." }
            nextStep = TaskStep(WaitCondition(ticks), continuation)
        }

    suspend fun wait(minTicks: Int, maxTicks: Int): Unit =
        suspendCoroutine { continuation ->
            require(minTicks > 0) { "Wait ticks must be greater than 0." }
            require(maxTicks > minTicks) { "maxTicks must be greater than minTicks." }
            nextStep = TaskStep(WaitCondition((minTicks..maxTicks).random()), continuation)
        }

    suspend fun waitUntilCycle(targetCycle: Long): Unit =
        waitUntil { GameCycleClock.currentCycle() >= targetCycle }

    fun currentCycle(): Long = GameCycleClock.currentCycle()

    suspend fun waitUntil(predicate: () -> Boolean): Unit =
        suspendCoroutine { continuation ->
            nextStep = TaskStep(PredicateCondition(predicate), continuation)
        }

    suspend fun <T> waitReturnValue(key: TaskRequestKey<T>): T {
        waitUntil { returnValues.containsKey(key) }
        @Suppress("UNCHECKED_CAST")
        return returnValues.remove(key) as T
    }

    suspend fun repeatWhile(
        delayTicks: Int,
        immediate: Boolean = false,
        canRepeat: suspend () -> Boolean,
        logic: suspend StopControlScope.() -> Unit,
        onFinished: (suspend GameTask.() -> Unit)? = null,
    ) {
        val scope = StopControlScope(this)
        if (immediate) {
            scope.logic()
        }
        while (canRepeat() && !scope.isStopped()) {
            wait(delayTicks)
            scope.logic()
        }
        onFinished?.invoke(this)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GameTask::class.java)
    }
}
