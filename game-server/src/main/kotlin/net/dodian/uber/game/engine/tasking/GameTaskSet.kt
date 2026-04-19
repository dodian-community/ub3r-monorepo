package net.dodian.uber.game.engine.tasking

import java.util.ArrayDeque
import kotlin.coroutines.createCoroutine
import net.dodian.uber.game.engine.metrics.TaskLifecycleTelemetry

abstract class GameTaskSet<OWNER : Any>(
    protected val owner: OWNER,
) {
    protected val queue = ArrayDeque<GameTask>()

    val size: Int
        get() = queue.size

    fun queue(
        priority: TaskPriority = TaskPriority.STANDARD,
        block: suspend GameTask.() -> Unit,
    ): TaskHandle {
        val control = TaskControl()
        val task = GameTask(owner, priority, control)
        TaskLifecycleTelemetry.recordScheduled()
        control.bindCancelAction { reason -> task.terminate(reason) }
        task.coroutine = block.createCoroutine(receiver = task, completion = task)

        if (priority == TaskPriority.STRONG) {
            cancelWeakerTasks()
        }

        queue.addLast(task)
        task.markPending()
        TaskLifecycleTelemetry.recordQueuePressure(owner::class.simpleName ?: "unknown", queue.size)
        return TaskHandle(control)
    }

    fun submitReturnValue(key: TaskRequestKey<*>, value: Any?) {
        cleanupFinished()
        queue.peekFirst()?.submitReturnValue(key, value)
    }

    fun terminateTasks() {
        queue.forEach { it.terminate("owner_terminated") }
        cleanupFinished()
    }

    protected fun cleanupFinished() {
        val iterator = queue.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().isFinished()) {
                iterator.remove()
            }
        }
    }

    private fun cancelWeakerTasks() {
        queue.forEach { task ->
            if (task.priority != TaskPriority.STRONG) {
                task.terminate("priority_preempted")
            }
        }
        cleanupFinished()
    }

    abstract fun cycle()
}
