package net.dodian.uber.game.runtime.task

internal class TaskControl {
    @Volatile
    var cancelled: Boolean = false
        private set

    @Volatile
    var completed: Boolean = false
        private set

    @Volatile
    private var cancelAction: (() -> Unit)? = null

    fun bindCancelAction(action: () -> Unit) {
        cancelAction = action
    }

    fun cancel() {
        if (cancelled) {
            return
        }
        cancelled = true
        cancelAction?.invoke()
    }

    fun markCompleted() {
        completed = true
    }
}

class TaskHandle internal constructor(
    private val control: TaskControl,
) {
    fun cancel() {
        control.cancel()
    }

    fun isCancelled(): Boolean = control.cancelled

    fun isCompleted(): Boolean = control.completed
}
