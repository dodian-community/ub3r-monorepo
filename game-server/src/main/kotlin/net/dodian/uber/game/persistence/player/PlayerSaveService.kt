package net.dodian.uber.game.persistence.player

import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.LockSupport
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import net.dodian.uber.game.engine.loop.TickThreadBlockingGuard
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.persistence.account.AccountPersistenceService
import net.dodian.uber.game.persistence.player.PlayerSaveReason
import net.dodian.uber.game.persistence.player.PlayerSaveSnapshot
import org.slf4j.LoggerFactory
import net.dodian.uber.game.engine.loop.GameThreadIngress

object PlayerSaveService {
    private val logger = LoggerFactory.getLogger(PlayerSaveService::class.java)
    private val sequence = AtomicLong(0)
    private val started = AtomicBoolean(false)
    private val shuttingDown = AtomicBoolean(false)
    private val pending = ConcurrentHashMap<Int, PlayerSaveRequest>()
    private val pendingFinalSaves = ConcurrentHashMap.newKeySet<Int>()
    private val activeDbId = AtomicInteger(-1)
    private val activeFinalDbId = AtomicInteger(-1)
    private val settlementDbIds = ConcurrentHashMap.newKeySet<Int>()
    private val activeSettlements = AtomicInteger(0)
    private val oldestQueuedAt = AtomicLong(0)
    private val lastWriteDurationMs = AtomicLong(0)
    private val totalRetries = AtomicLong(0)
    private val repository = PlayerSaveSqlRepository()

    @Volatile
    private var worker: Job? = null

    @JvmStatic
    fun requestSave(
        client: Client,
        reason: PlayerSaveReason,
        updateProgress: Boolean,
        finalSave: Boolean,
    ) {
        if (client.dbId < 1) {
            return
        }
        if (client.dbId in settlementDbIds) {
            // Keep the dirty bits set. A fresh snapshot will be requested after
            // the paired commit publishes its identical live inventory image.
            return
        }
        val dirtyMask =
            when {
                finalSave || updateProgress -> PlayerSaveSegment.ALL_MASK
                client.saveDirtyMask == 0 -> 0
                else -> client.saveDirtyMask
            }
        if (dirtyMask == 0 && !finalSave) {
            return
        }

        val seq = sequence.incrementAndGet()
        val envelope = PlayerSaveEnvelope.fromClient(client, seq, reason, updateProgress, finalSave, dirtyMask)
        val shadowSnapshot =
            if (SAVE_SHADOW_ENABLED) {
                PlayerSaveSnapshot.fromClient(client, seq, reason, updateProgress, finalSave)
            } else {
                null
            }

        queue(PlayerSaveRequest(envelope = envelope, shadowSnapshot = shadowSnapshot))
        if (!finalSave) {
            client.clearSaveDirtyMask(dirtyMask)
        }
    }

    @JvmStatic
    fun saveSynchronously(
        client: Client,
        reason: PlayerSaveReason,
        updateProgress: Boolean,
        finalSave: Boolean,
    ) {
        TickThreadBlockingGuard.requireNotGameThread("PlayerSaveService.saveSynchronously")
        val dirtyMask =
            if (finalSave || updateProgress) PlayerSaveSegment.ALL_MASK else client.saveDirtyMask
        if (dirtyMask == 0 && !finalSave) {
            return
        }
        val envelope =
            PlayerSaveEnvelope.fromClient(
                client = client,
                sequence = sequence.incrementAndGet(),
                reason = reason,
                updateProgress = updateProgress,
                finalSave = finalSave,
                dirtyMask = dirtyMask,
            )
        repository.saveEnvelope(envelope)
        client.clearAllSaveDirty()
    }

    /**
     * Persists two projected post-exchange inventories in one SQL transaction.
     * Older queued snapshots are superseded and newer snapshots cannot cross
     * the barrier until the game-thread completion callback has published the
     * exact same images.
     */
    @JvmStatic
    fun requestPairedInventorySettlement(
        first: Client,
        firstItemIds: IntArray,
        firstAmounts: IntArray,
        second: Client,
        secondItemIds: IntArray,
        secondAmounts: IntArray,
        reason: PlayerSaveReason,
        onCommitted: Runnable,
        onFailure: Runnable,
    ): Boolean {
        if (first.dbId < 1 || second.dbId < 1 || first.dbId == second.dbId) return false
        val requestedAt = System.nanoTime()
        val dbIds = setOf(first.dbId, second.dbId)
        synchronized(settlementDbIds) {
            if (dbIds.any { it in settlementDbIds }) return false
            settlementDbIds.addAll(dbIds)
        }

        val firstEnvelope = PlayerSaveEnvelope.fromClient(
            first, sequence.incrementAndGet(), reason, false, false, PlayerSaveSegment.ALL_MASK,
        ).withInventory(firstItemIds.copyOf(), firstAmounts.copyOf())
        val secondEnvelope = PlayerSaveEnvelope.fromClient(
            second, sequence.incrementAndGet(), reason, false, false, PlayerSaveSegment.ALL_MASK,
        ).withInventory(secondItemIds.copyOf(), secondAmounts.copyOf())

        dbIds.forEach {
            pending.remove(it)
            pendingFinalSaves.remove(it)
        }
        activeSettlements.incrementAndGet()
        AccountPersistenceService.scope.launch {
            try {
                while (isActive && activeDbId.get() in dbIds) delay(10L)
                val writeStartedAt = System.nanoTime()
                repository.saveEnvelopesAtomically(listOf(firstEnvelope, secondEnvelope))
                val writeFinishedAt = System.nanoTime()
                submitSettlementCallback(dbIds, Runnable {
                    val publishStartedAt = System.nanoTime()
                    onCommitted.run()
                    logger.info(
                        "exchange_settlement_committed reason={} players={} queue_ms={} write_ms={} publish_ms={} total_ms={}",
                        reason,
                        dbIds.sorted(),
                        elapsedMillis(requestedAt, writeStartedAt),
                        elapsedMillis(writeStartedAt, writeFinishedAt),
                        elapsedMillis(publishStartedAt),
                        elapsedMillis(requestedAt),
                    )
                })
            } catch (exception: Exception) {
                logger.error(
                    "exchange_settlement_failed reason={} players={} elapsed_ms={}",
                    reason,
                    dbIds.sorted(),
                    elapsedMillis(requestedAt),
                    exception,
                )
                submitSettlementCallback(dbIds, onFailure)
            } finally {
                activeSettlements.decrementAndGet()
            }
        }
        return true
    }

    @JvmStatic
    fun isFinalSavePending(dbId: Int): Boolean =
        pendingFinalSaves.contains(dbId) || activeFinalDbId.get() == dbId

    @JvmStatic
    fun shutdownAndDrain(timeout: Duration) {
        TickThreadBlockingGuard.requireNotGameThread("PlayerSaveService.shutdownAndDrain")
        shuttingDown.set(true)
        val deadline = System.nanoTime() + timeout.toNanos()
        while (System.nanoTime() < deadline) {
            if (pending.isEmpty() && activeDbId.get() == -1 && activeSettlements.get() == 0) {
                break
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(25L))
        }

        val remainingNanos = (deadline - System.nanoTime()).coerceAtLeast(1L)
        val remainingMillis = TimeUnit.NANOSECONDS.toMillis(remainingNanos).coerceAtLeast(1L)
        val currentWorker = worker
        if (currentWorker != null) {
            val latch = CountDownLatch(1)
            currentWorker.invokeOnCompletion { latch.countDown() }
            currentWorker.cancel()
            val drainedCleanly = latch.await(remainingMillis, TimeUnit.MILLISECONDS)
            if (!drainedCleanly) {
                logger.warn(
                    "Shutdown drain timed out after {}ms waiting for the save worker to finish; " +
                        "some in-flight player saves may not have completed",
                    timeout.toMillis(),
                )
            }
        }
    }

    @JvmStatic
    fun getQueueDepth(): Int = pending.size

    @JvmStatic
    fun getOldestQueuedAgeMs(): Long {
        val queuedAt = oldestQueuedAt.get()
        if (queuedAt == 0L || pending.isEmpty()) {
            return 0L
        }
        return System.currentTimeMillis() - queuedAt
    }

    @JvmStatic
    fun getRetryCount(): Long = totalRetries.get()

    @JvmStatic
    fun getLastWriteDurationMs(): Long = lastWriteDurationMs.get()

    private fun queue(request: PlayerSaveRequest) {
        if (shuttingDown.get() && !request.envelope.finalSave) {
            return
        }
        ensureStarted()
        val dbId = request.envelope.dbId
        val chosen =
            pending.compute(dbId) { _, existing ->
                if (existing == null) {
                    request
                } else if (existing.envelope.finalSave && !request.envelope.finalSave) {
                    // Never allow a periodic save to overwrite a queued final save.
                    existing
                } else if (!existing.envelope.finalSave && request.envelope.finalSave) {
                    request
                } else {
                    if (request.envelope.sequence >= existing.envelope.sequence) request else existing
                }
            } ?: request
        if (chosen.envelope.finalSave) {
            pendingFinalSaves += dbId
        } else {
            pendingFinalSaves.remove(dbId)
        }
        oldestQueuedAt.compareAndSet(0L, System.currentTimeMillis())
    }

    private fun ensureStarted() {
        if (!started.compareAndSet(false, true)) {
            return
        }
        worker =
            AccountPersistenceService.scope.launch {
                while (isActive) {
                    drainOnce()
                    delay(SAVE_BATCH_DELAY_MS)
                }
            }
    }

    private suspend fun drainOnce() {
        while (true) {
            val next = nextPendingRequest() ?: break
            val dbId = next.envelope.dbId
            activeDbId.set(dbId)
            activeFinalDbId.set(if (next.envelope.finalSave) dbId else -1)
            try {
                handleRequest(next)
            } finally {
                activeDbId.set(-1)
                activeFinalDbId.set(-1)
                val stillFinalQueued = pending[dbId]?.envelope?.finalSave == true
                if (!stillFinalQueued) {
                    pendingFinalSaves.remove(dbId)
                }
                if (pending.isEmpty()) {
                    oldestQueuedAt.set(0L)
                }
            }
        }
    }

    private fun nextPendingRequest(): PlayerSaveRequest? {
        val next = pending.entries
            .asSequence()
            .filter { it.key !in settlementDbIds }
            .minByOrNull { it.value.envelope.sequence }
            ?: return null
        val removed = pending.remove(next.key, next.value)
        if (!removed) {
            return null
        }
        return next.value
    }

    private suspend fun handleRequest(request: PlayerSaveRequest) {
        var backoffMs = SAVE_RETRY_BASE_MS.coerceAtLeast(50L)
        while (AccountPersistenceService.scope.isActive) {
            val elapsed =
                withTimeoutOrNull(SAVE_REQUEST_TIMEOUT_MS) {
                    measureTimeMillis {
                        if (request.envelope.finalSave || request.envelope.updateProgress) {
                            request.shadowSnapshot?.let { shadow ->
                                compareShadow(shadow, repository.buildSnapshot(request.envelope))
                            }
                        }
                        repository.saveEnvelope(request.envelope)
                    }
                }

            if (elapsed != null) {
                lastWriteDurationMs.set(elapsed)
                return
            }

            request.attempts++
            totalRetries.incrementAndGet()
            if (!request.envelope.finalSave && request.attempts >= SAVE_BURST_ATTEMPTS.coerceAtLeast(1)) {
                logger.error(
                    "Save failed after {} attempts for {} (dbId={})",
                    request.attempts,
                    request.envelope.playerName,
                    request.envelope.dbId,
                )
                return
            }

            logger.warn(
                "Retrying save attempt {} for {} (dbId={})",
                request.attempts,
                request.envelope.playerName,
                request.envelope.dbId,
            )
            delay(timeMillis = backoffMs)
            backoffMs = (backoffMs * 2).coerceAtMost(SAVE_RETRY_MAX_MS)
        }
    }

    private fun compareShadow(oldSnapshot: PlayerSaveSnapshot, newSnapshot: PlayerSaveSnapshot) {
        val same =
            oldSnapshot.statsUpdateSql == newSnapshot.statsUpdateSql &&
                oldSnapshot.statsProgressInsertSql == newSnapshot.statsProgressInsertSql &&
                oldSnapshot.characterUpdateSql == newSnapshot.characterUpdateSql
        if (!same) {
            logger.warn(
                "Player save shadow mismatch for {} (dbId={}, reason={})",
                newSnapshot.playerName,
                newSnapshot.dbId,
                newSnapshot.reason,
            )
        }
    }

    private suspend fun submitSettlementCallback(dbIds: Set<Int>, callback: Runnable) {
        while (AccountPersistenceService.scope.isActive) {
            val accepted = GameThreadIngress.submitCritical("exchange-settlement") {
                try {
                    callback.run()
                } finally {
                    dbIds.forEach(settlementDbIds::remove)
                }
            }
            if (accepted) return
            delay(10L)
        }
        dbIds.forEach(settlementDbIds::remove)
    }

    private fun elapsedMillis(startedAt: Long, finishedAt: Long = System.nanoTime()): Double =
        (finishedAt - startedAt) / 1_000_000.0

    private const val SAVE_BURST_ATTEMPTS = 8
    private const val SAVE_RETRY_BASE_MS = 250L
    private const val SAVE_RETRY_MAX_MS = 5000L
    private const val SAVE_BATCH_DELAY_MS = 100L
    private const val SAVE_REQUEST_TIMEOUT_MS = 5000L
    private const val SAVE_SHADOW_ENABLED = false
}
