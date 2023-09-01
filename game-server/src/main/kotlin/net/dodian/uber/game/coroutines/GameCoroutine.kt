package net.dodian.uber.game.coroutines

import net.dodian.uber.game.coroutines.resume.DeferResumeCondition
import net.dodian.uber.game.coroutines.resume.PredicateResumeCondition
import net.dodian.uber.game.coroutines.resume.TickResumeCondition
import kotlin.coroutines.RestrictsSuspension
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST", "MemberVisibilityCanBePrivate")
@RestrictsSuspension
class GameCoroutine(public val debugName: String? = null) {

    private var suspension: GameCoroutineSuspension<Any>? = null

    val isIdle: Boolean get() = !isSuspended

    val isSuspended: Boolean get() = suspension != null

    fun resume() {
        val suspension = suspension ?: return
        val resume = suspension.resume()
        if (resume && this.suspension === suspension) {
            this.suspension = null
        }
    }

    fun stop(): Nothing {
        suspension = null
        throw CancellationException()
    }

    fun cancel(exception: CancellationException = CancellationException()) {
        suspension?.continuation?.resumeWithException(exception)
        suspension = null
    }

    fun resumeWith(value: Any) {
        val condition = suspension?.condition ?: error("Coroutine not suspended: $this")
        if (condition !is DeferResumeCondition) return
        if (condition.type != value::class) return
        condition.set(value)
        resume()
    }

    suspend fun pause(ticks: Int) {
        if (ticks <= 0) return
        suspendCoroutineUninterceptedOrReturn {
            val condition = TickResumeCondition(ticks)
            suspension = GameCoroutineSuspension(it, condition) as GameCoroutineSuspension<Any>
            COROUTINE_SUSPENDED
        }
    }

    suspend fun pause(resume: () -> Boolean) {
        if (resume()) return
        suspendCoroutineUninterceptedOrReturn {
            val condition = PredicateResumeCondition(resume)
            suspension = GameCoroutineSuspension(it, condition) as GameCoroutineSuspension<Any>
            COROUTINE_SUSPENDED
        }
    }

    suspend fun <T : Any> pause(type: KClass<T>): T {
        return suspendCoroutineUninterceptedOrReturn {
            val condition = DeferResumeCondition(type)
            suspension = GameCoroutineSuspension(it, condition) as GameCoroutineSuspension<Any>
            COROUTINE_SUSPENDED
        }
    }

    override fun toString(): String {
        return "GameCoroutine(debugName=$debugName, suspension=$suspension)"
    }

    private companion object {

        private fun <T> GameCoroutineSuspension<T>.resume(): Boolean {
            val deferred = condition.resumeOrNull() ?: return false
            continuation.resume(deferred)
            return true
        }
    }
}
