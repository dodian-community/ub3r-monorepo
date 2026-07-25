package net.dodian.uber.game.engine.sync

import java.util.concurrent.Callable
import java.util.concurrent.ForkJoinPool
import org.slf4j.LoggerFactory

/**
 * rsprot-style fan-out/join worker for per-viewer sync encoding (mirrors
 * DefaultProtocolWorker): below [asynchronousThreshold] viewers the work runs inline
 * on the game thread — bitwise-identical to the historical sequential path — and above
 * it the per-viewer callables are submitted to [ForkJoinPool.commonPool] as one
 * submit-and-join barrier.
 *
 * Safety contract (verified against the pipeline):
 *  - PREP phases (shared block caches, viewport snapshots) complete before this runs.
 *  - Encode scratch is thread-local; each pooled worker lazily gets its own.
 *  - Each action must be self-isolating (the encode loops already try/catch per viewer
 *    and only mark that one viewer disconnected on failure).
 *  - [SynchronizationContext] is ThreadLocal, so the current cycle is re-bound inside
 *    every pooled task and cleared afterwards (pool threads are reused).
 *
 * Kill-switch: env SYNC_PARALLEL_ENCODE=false forces the inline path.
 */
object SyncWorker {
    private val logger = LoggerFactory.getLogger(SyncWorker::class.java)

    private val asynchronousThreshold = Runtime.getRuntime().availableProcessors() * 4

    private val parallelEnabled: Boolean =
        System.getenv().getOrDefault("SYNC_PARALLEL_ENCODE", "true").equals("true", ignoreCase = true)



    /**
     * Runs [action] for every element of [viewers]; inline when below the threshold or
     * disabled, otherwise fanned out over the common pool with a join barrier. Actions
     * must not throw (per-viewer isolation belongs inside them); an escape is rethrown
     * after the barrier completes so the tick fails loudly rather than silently.
     */
    @JvmStatic
    fun <T> forEachViewer(viewers: List<T>, action: (T) -> Unit) {
        if (!parallelEnabled || viewers.size < asynchronousThreshold) {
            viewers.forEach(action)
            return
        }
        val cycle = SynchronizationContext.current()
        val callables = ArrayList<Callable<Unit>>(viewers.size)
        for (viewer in viewers) {
            callables.add(
                Callable {
                    if (cycle != null) SynchronizationContext.setCurrent(cycle)
                    try {
                        action(viewer)
                    } finally {
                        SynchronizationContext.clear()
                    }
                },
            )
        }
        val futures = ForkJoinPool.commonPool().invokeAll(callables)
        for (future in futures) {
            // Surfaces anything that escaped a per-viewer isolation block (should be never).
            future.get()
        }
    }
}
