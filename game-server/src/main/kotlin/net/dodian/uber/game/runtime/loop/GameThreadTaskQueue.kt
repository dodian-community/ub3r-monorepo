package net.dodian.uber.game.runtime.loop

object GameThreadTaskQueue {
    @JvmStatic
    fun submit(task: Runnable) {
        submit("anonymous", task)
    }

    @JvmStatic
    fun submit(label: String, task: Runnable) {
        GameThreadIngress.submitDeferred(label, task)
    }

    @JvmStatic
    fun drain() {
        drain(10_000)
    }

    @JvmStatic
    fun drain(maxTasks: Int) {
        GameThreadIngress.drainDeferred(maxTasks)
    }

    @JvmStatic
    fun clearForTests() {
        GameThreadIngress.clearForTests()
    }
}
