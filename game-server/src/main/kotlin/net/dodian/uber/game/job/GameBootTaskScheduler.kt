package net.dodian.uber.game.job

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dodian.uber.game.dispatcher.io.IOCoroutineScope

private typealias Task = suspend CoroutineScope.() -> Unit
class GameBootTaskScheduler(private val ioCoroutineScope: IOCoroutineScope) {

    private val _blocking: MutableList<Task> = mutableListOf()
    private val _nonBlocking: MutableList<Task> = mutableListOf()

    val blocking: List<suspend CoroutineScope.() -> Unit> get() = _blocking
    val nonBlocking: List<suspend CoroutineScope.() -> Unit> get() = _nonBlocking

    fun scheduleBlocking(action: suspend CoroutineScope.() -> Unit) {
        _blocking += action
    }

    fun scheduleNonBlocking(action: suspend CoroutineScope.() -> Unit) {
        _nonBlocking += action
    }

    suspend fun executeBlocking(scope: CoroutineScope) {
        _blocking.forEach { it.invoke(scope) }
    }

    suspend fun executeNonBlocking() {
        ioCoroutineScope.executeNonBlocking().join()
    }

    private fun CoroutineScope.executeNonBlocking() = launch {
        _nonBlocking.forEach { launch { it(this) } }
    }
}