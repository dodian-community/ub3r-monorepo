package net.dodian.uber.game.runtime.scheduler

import net.dodian.uber.game.runtime.tasking.TaskHandle

class QueueTaskHandle internal constructor(
    private val delegate: TaskHandle,
) {
    fun cancel() {
        delegate.cancel()
    }

    fun isCancelled(): Boolean = delegate.isCancelled()

    fun isCompleted(): Boolean = delegate.isCompleted()

    companion object {
        @JvmStatic
        fun from(handle: TaskHandle): QueueTaskHandle = QueueTaskHandle(handle)
    }
}
