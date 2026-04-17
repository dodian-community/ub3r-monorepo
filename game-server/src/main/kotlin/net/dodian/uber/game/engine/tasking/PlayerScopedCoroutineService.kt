package net.dodian.uber.game.engine.tasking

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.dodian.uber.game.model.entity.player.Client

/**
 * Tracks coroutine jobs scoped to a player and lightweight job-key.
 *
 * This prevents leaked background work after logout/disconnect and provides
 * deterministic key replacement for idempotent tasks (e.g. "refund-check").
 */
object PlayerScopedCoroutineService {
    private val jobsByPlayer = ConcurrentHashMap<Client, ConcurrentHashMap<String, Job>>()

    @JvmStatic
    fun launch(
        player: Client,
        jobKey: String,
        scope: CoroutineScope,
        block: suspend CoroutineScope.() -> Unit,
    ): Job {
        val playerJobs = jobsByPlayer.computeIfAbsent(player) { ConcurrentHashMap() }
        playerJobs.remove(jobKey)?.cancel(CancellationException("Replaced player-scoped job: $jobKey"))

        val job = scope.launch(block = block)
        playerJobs[jobKey] = job
        job.invokeOnCompletion {
            val current = jobsByPlayer[player] ?: return@invokeOnCompletion
            current.remove(jobKey, job)
            if (current.isEmpty()) {
                jobsByPlayer.remove(player, current)
            }
        }
        return job
    }

    @JvmStatic
    fun cancelForPlayer(
        player: Client,
        reason: String = "Player lifecycle ended",
    ) {
        val jobs = jobsByPlayer.remove(player) ?: return
        val cancellation = CancellationException(reason)
        jobs.values.forEach { job -> job.cancel(cancellation) }
    }

    @JvmStatic
    fun activeJobCount(player: Client): Int = jobsByPlayer[player]?.size ?: 0
}
