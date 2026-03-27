package net.dodian.uber.game.runtime.tasking

import java.util.ArrayDeque
import kotlin.coroutines.createCoroutine

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
        control.bindCancelAction { task.terminate() }
        task.coroutine = block.createCoroutine(receiver = task, completion = task)

        if (priority == TaskPriority.STRONG) {
            cancelWeakerTasks()
        }

        queue.addLast(task)
        return TaskHandle(control)
    }

    fun submitReturnValue(key: TaskRequestKey<*>, value: Any?) {
        cleanupFinished()
        queue.peekFirst()?.submitReturnValue(key, value)
    }

    fun terminateTasks() {
        queue.forEach { it.terminate() }
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
                task.terminate()
            }
        }
        cleanupFinished()
    }

    abstract fun cycle()
}
