package net.dodian.uber.game.coroutines

import net.dodian.uber.game.coroutines.complete.GameCoroutineSimpleCompletion
import org.rsmod.game.coroutines.throwable.ScopeCancellationException
import kotlin.coroutines.Continuation
import kotlin.coroutines.startCoroutine

class GameCoroutineScope {

    private val _children = mutableListOf<GameCoroutine>()
    val children: List<GameCoroutine> get() = _children

    fun launch(
        coroutine: GameCoroutine = GameCoroutine(),
        completion: Continuation<Unit> = GameCoroutineSimpleCompletion,
        block: suspend GameCoroutine.() -> Unit
    ): GameCoroutine {
        block.startCoroutine(coroutine, completion)
        if (coroutine.isSuspended) _children += coroutine
        return coroutine
    }

    fun advance() {
        _children.forEach { it.resume() }
        _children.removeIf { it.isIdle }
    }

    fun cancel() {
        _children.forEach { it.cancel(ScopeCancellationException) }
        _children.clear()
    }
}
