package net.dodian.uber.game.persistence

import java.util.concurrent.atomic.AtomicBoolean
import net.dodian.uber.game.persistence.world.WorldDbPollService
import net.dodian.uber.game.persistence.world.WorldPollInput
import net.dodian.uber.game.persistence.world.WorldPollResult
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

object WorldPollPublisher {
    private val logger = LoggerFactory.getLogger(WorldPollPublisher::class.java)
    private val scope = CoroutineScope(SupervisorJob() + DbDispatchers.worldDispatcher)
    private val running = AtomicBoolean(true)
    private val workerScheduled = AtomicBoolean(false)
    private val latestSnapshot = AtomicReference<WorldPollSnapshot?>()

    @JvmStatic
    fun publish(snapshot: WorldPollSnapshot) {
        if (!running.get()) {
            return
        }
        latestSnapshot.set(snapshot)
        scheduleWorker()
    }

    @JvmStatic
    fun latestResult(): WorldPollResult = WorldPollResultStore.latest()

    @JvmStatic
    fun shutdown() {
        running.set(false)
        latestSnapshot.set(null)
        scope.cancel("World poll publisher shutdown")
    }

    private fun scheduleWorker() {
        if (!workerScheduled.compareAndSet(false, true)) {
            return
        }
        scope.launch {
            try {
                while (running.get()) {
                    val snapshot = latestSnapshot.getAndSet(null) ?: break
                    try {
                        WorldPollResultStore.publish(WorldDbPollService.runBlockingPoll(snapshot.toInput()))
                    } catch (_: CancellationException) {
                        break
                    } catch (exception: RuntimeException) {
                        logger.error("World poll publisher failed", exception)
                    }
                }
            } finally {
                workerScheduled.set(false)
                if (running.get() && latestSnapshot.get() != null) {
                    scheduleWorker()
                }
            }
        }
    }
}
