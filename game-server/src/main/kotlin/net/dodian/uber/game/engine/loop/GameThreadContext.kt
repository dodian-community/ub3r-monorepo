package net.dodian.uber.game.engine.loop

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import net.dodian.uber.game.engine.metrics.OperationalTelemetry
import net.dodian.uber.game.engine.config.serverDebugMode
import org.slf4j.LoggerFactory

object GameThreadContext {
    private val logger = LoggerFactory.getLogger(GameThreadContext::class.java)
    private val gameThreadRef = AtomicReference<Thread?>()
    private val nextLogAtByContext = ConcurrentHashMap<String, AtomicLong>()
    private val totalViolations = AtomicLong()
    private val unboundViolations = AtomicLong()
    private val wrongThreadViolations = AtomicLong()

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

    /**
     * Reports a beta thread-ownership violation without changing control flow.
     * This deliberately remains diagnostic-only until live telemetry is clean.
     */
    @JvmStatic
    fun validateGameThread(context: String): Boolean {
        val expected = gameThreadRef.get()
        val current = Thread.currentThread()
        if (expected != null && current === expected) return true

        val reason = if (expected == null) "unbound" else "wrong_thread"
        totalViolations.incrementAndGet()
        if (expected == null) unboundViolations.incrementAndGet() else wrongThreadViolations.incrementAndGet()
        OperationalTelemetry.incrementCounter("thread_ownership.violation")
        OperationalTelemetry.incrementCounter("thread_ownership.$reason")

        val safeContext = context.ifBlank { "unknown" }
        val debug = forceDebugModeForTests ?: (try { serverDebugMode } catch (_: Exception) { false })
        if (debug) {
            throw IllegalStateException(
                "Game-thread ownership violation reason=$reason context=$safeContext " +
                "expected=${expected?.name ?: "<unbound>"} actual=${current.name}"
            )
        }

        val nextLogAt = nextLogAtByContext.computeIfAbsent("$reason:$safeContext") { AtomicLong() }
        val now = System.currentTimeMillis()
        val allowedAt = nextLogAt.get()
        if (now >= allowedAt && nextLogAt.compareAndSet(allowedAt, now + LOG_INTERVAL_MS)) {
            logger.error(
                "Game-thread ownership violation reason={} context={} expected={} actual={}",
                reason,
                safeContext,
                expected?.name ?: "<unbound>",
                current.name,
                IllegalStateException("Game-thread ownership violation: $safeContext"),
            )
        }
        return false
    }

    @JvmStatic fun violationCount(): Long = totalViolations.get()
    @JvmStatic fun unboundViolationCount(): Long = unboundViolations.get()
    @JvmStatic fun wrongThreadViolationCount(): Long = wrongThreadViolations.get()

    @JvmStatic
    internal fun clearBindingForTests() {
        gameThreadRef.set(null)
    }

    @JvmStatic
    internal fun resetDiagnosticsForTests() {
        totalViolations.set(0L)
        unboundViolations.set(0L)
        wrongThreadViolations.set(0L)
        nextLogAtByContext.clear()
        forceDebugModeForTests = null
    }

    @JvmStatic
    internal var forceDebugModeForTests: Boolean? = null

    private const val LOG_INTERVAL_MS = 60_000L
}
