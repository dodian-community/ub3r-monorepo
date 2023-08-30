package net.dodian.uber.net.message

abstract class Message(
    private var terminated: Boolean = false
) {
    fun terminate() {
        terminated = true
    }

    val isTerminated get() = terminated
}