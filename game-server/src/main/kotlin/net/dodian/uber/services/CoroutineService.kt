package net.dodian.uber.services

import com.github.michaelbull.logging.InlineLogger
import kotlinx.coroutines.*
import net.dodian.uber.game.dispatcher.main.MainCoroutineScope
import net.dodian.uber.game.job.GameBootTaskScheduler
import net.dodian.uber.game.process.GameProcess
import java.util.concurrent.TimeUnit
import kotlin.system.measureNanoTime

private const val GAME_TICK_DELAY = 600

private val logger = InlineLogger()

class CoroutineService(
    private val process: GameProcess,
    private val bootTasks: GameBootTaskScheduler,
    private val coroutineScope: MainCoroutineScope,
) : IdleService() {

    private var excessCycleNanos = 0L

    private fun CoroutineScope.start(delay: Int) = launch {
        while (isActive) {
            val elapsedNanos = measureNanoTime { process.cycle() } + excessCycleNanos
            val elapsedMillis = TimeUnit.NANOSECONDS.toMillis(elapsedNanos)
            val overdue = elapsedMillis > delay
            val sleepTime = if (overdue) {
                val elapsedCycleCount = elapsedMillis / delay
                val upcomingCycleDelay = (elapsedCycleCount + 1) * delay
                upcomingCycleDelay - elapsedMillis
            } else {
                delay - elapsedMillis
            }
            if (overdue) {
                logger.error { "Cycle took too long (elapsed=${elapsedMillis}ms, sleep=${sleepTime}ms)" }
            } else {
                //logger.debug { "Cycle took ${elapsedMillis}ms to complete, slept for ${sleepTime}ms" }
            }
            excessCycleNanos = elapsedNanos - TimeUnit.MILLISECONDS.toNanos(elapsedMillis)
            delay(sleepTime)
        }
    }

    private fun GameBootTaskScheduler.execute(): Unit = runBlocking {
        executeNonBlocking()
        executeBlocking(this)
    }

    override fun startUp() {
        bootTasks.execute()
        process.startUp()
        coroutineScope.start(GAME_TICK_DELAY)
    }

    override fun shutDown() {
        if (isRunning) {
            coroutineScope.cancel()
            process.shutDown()
        }
    }
}