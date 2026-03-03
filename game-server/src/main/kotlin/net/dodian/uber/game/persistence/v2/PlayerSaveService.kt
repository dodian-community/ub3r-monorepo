package net.dodian.uber.game.persistence.v2

import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.persistence.account.AccountPersistenceService
import net.dodian.uber.game.persistence.PlayerSaveReason
import net.dodian.uber.game.persistence.PlayerSaveSnapshot
import org.slf4j.LoggerFactory
import net.dodian.utilities.databaseSaveBurstAttempts
import net.dodian.utilities.databaseSaveRetryBaseMs
import net.dodian.utilities.databaseSaveRetryMaxMs
import net.dodian.utilities.playerSaveBatchDelayMs
import net.dodian.utilities.playerSaveRequestTimeoutMs
import net.dodian.utilities.playerSaveShadowEnabled

object PlayerSaveService {
    private val logger = LoggerFactory.getLogger(PlayerSaveService::class.java)
    private val sequence = AtomicLong(0)
    private val started = AtomicBoolean(false)
    private val shuttingDown = AtomicBoolean(false)
    private val pending = ConcurrentHashMap<Int, PlayerSaveRequest>()
    private val pendingFinalSaves = ConcurrentHashMap.newKeySet<Int>()
    private val activeDbId = AtomicInteger(-1)
    private val oldestQueuedAt = AtomicLong(0)
    private val lastWriteDurationMs = AtomicLong(0)
    private val totalRetries = AtomicLong(0)
    private val repository = PlayerSaveRepositoryV2()

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
            if (playerSaveShadowEnabled) {
                PlayerSaveSnapshot.fromClient(client, seq, reason, updateProgress, finalSave)
            } else {
                null
            }

        queue(PlayerSaveRequest(envelope = envelope, shadowSnapshot = shadowSnapshot))
    }

    @JvmStatic
    fun saveSynchronously(
        client: Client,
        reason: PlayerSaveReason,
        updateProgress: Boolean,
        finalSave: Boolean,
    ) {
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

    @JvmStatic
    fun isFinalSavePending(dbId: Int): Boolean =
        pendingFinalSaves.contains(dbId) || activeDbId.get() == dbId

    @JvmStatic
    fun shutdownAndDrain(timeout: Duration) {
        shuttingDown.set(true)
        val deadline = System.nanoTime() + timeout.toNanos()
        while (System.nanoTime() < deadline) {
            if (pending.isEmpty() && activeDbId.get() == -1) {
                break
            }
            Thread.sleep(25L)
        }

        runBlocking {
            worker?.cancel()
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
        pending[request.envelope.dbId] = request
        if (request.envelope.finalSave) {
            pendingFinalSaves += request.envelope.dbId
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
                    delay(playerSaveBatchDelayMs)
                }
            }
    }

    private suspend fun drainOnce() {
        while (true) {
            val next = nextPendingRequest() ?: break
            val dbId = next.envelope.dbId
            activeDbId.set(dbId)
            try {
                handleRequest(next)
            } finally {
                activeDbId.set(-1)
                if (!pending.containsKey(dbId)) {
                    pendingFinalSaves.remove(dbId)
                }
                if (pending.isEmpty()) {
                    oldestQueuedAt.set(0L)
                }
            }
        }
    }

    private fun nextPendingRequest(): PlayerSaveRequest? {
        val next = pending.entries.minByOrNull { it.value.envelope.sequence } ?: return null
        val removed = pending.remove(next.key, next.value)
        if (!removed) {
            return null
        }
        return next.value
    }

    private suspend fun handleRequest(request: PlayerSaveRequest) {
        var backoffMs = databaseSaveRetryBaseMs.coerceAtLeast(50L)
        while (AccountPersistenceService.scope.isActive) {
            val elapsed =
                withTimeoutOrNull(playerSaveRequestTimeoutMs) {
                    measureTimeMillis {
                        val snapshot = repository.buildSnapshot(request.envelope)
                        request.shadowSnapshot?.let { shadow -> compareShadow(shadow, snapshot) }
                        repository.saveEnvelope(request.envelope)
                    }
                }

            if (elapsed != null) {
                lastWriteDurationMs.set(elapsed)
                return
            }

            request.attempts++
            totalRetries.incrementAndGet()
            if (!request.envelope.finalSave && request.attempts >= databaseSaveBurstAttempts.coerceAtLeast(1)) {
                logger.error(
                    "V2 save failed after {} attempts for {} (dbId={})",
                    request.attempts,
                    request.envelope.playerName,
                    request.envelope.dbId,
                )
                return
            }

            logger.warn(
                "Retrying V2 save attempt {} for {} (dbId={})",
                request.attempts,
                request.envelope.playerName,
                request.envelope.dbId,
            )
            delay(timeMillis = backoffMs)
            backoffMs = (backoffMs * 2).coerceAtMost(databaseSaveRetryMaxMs)
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
}
