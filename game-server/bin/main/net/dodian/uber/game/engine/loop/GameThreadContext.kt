package net.dodian.uber.game.engine.loop

import java.util.concurrent.atomic.AtomicReference

object GameThreadContext {
    private val gameThreadRef = AtomicReference<Thread?>()

    @JvmStatic
    fun bindCurrentThread() {
        gameThreadRef.set(Thread.currentThread())
    }

    @JvmStatic
    fun isGameThread(): Boolean = Thread.currentThread() === gameThreadRef.get()

    @JvmStatic
    fun requireGameThread(context: String) {
        check(isGameThread()) { "Expected game thread: $context" }
    }

    @JvmStatic
    internal fun clearBindingForTests() {
        gameThreadRef.set(null)
    }
}
