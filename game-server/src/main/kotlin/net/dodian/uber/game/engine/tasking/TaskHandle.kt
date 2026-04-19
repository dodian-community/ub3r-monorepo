package net.dodian.uber.game.engine.tasking

internal class TaskControl {
    @Volatile
    var cancelled: Boolean = false
        private set

    @Volatile
    var completed: Boolean = false
        private set

    @Volatile
    private var cancelAction: ((String) -> Unit)? = null

    fun bindCancelAction(action: (String) -> Unit) {
        cancelAction = action
    }

    fun cancel(reason: String = "manual") {
        if (cancelled) {
            return
        }
        cancelled = true
        cancelAction?.invoke(reason)
    }

    fun markCompleted() {
        completed = true
    }
}

class TaskHandle internal constructor(
    private val control: TaskControl,
) {
    fun cancel(reason: String = "manual") {
        control.cancel(reason)
    }

    fun isCancelled(): Boolean = control.cancelled

    fun isCompleted(): Boolean = control.completed
}
