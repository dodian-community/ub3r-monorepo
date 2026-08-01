package net.dodian.uber.game.engine.sync

import kotlin.system.measureNanoTime
import net.dodian.uber.game.engine.config.gameMaxPlayers
import net.dodian.uber.game.engine.config.serverDebugMode
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.npc.NpcUpdating
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.model.entity.player.PlayerUpdating
import net.dodian.uber.game.engine.sync.cache.RootSynchronizationCache
import net.dodian.uber.game.engine.sync.npc.StagedNpcSynchronizationService
import net.dodian.uber.game.engine.sync.player.PlayerSyncInvariantValidator
import net.dodian.uber.game.engine.sync.player.StagedPlayerSynchronizationService
import net.dodian.uber.game.ui.PlayerUiDeltaProcessor
import net.dodian.uber.game.engine.sync.viewport.ViewportIndex
import net.dodian.uber.game.engine.loop.GameThreadContext
import net.dodian.uber.game.engine.processing.EntityProcessor
import net.dodian.uber.game.engine.systems.zone.ZoneUpdateBus
import net.dodian.uber.game.engine.config.runtimePhaseWarnMs
import net.dodian.uber.game.engine.metrics.OperationalTelemetry
import org.slf4j.LoggerFactory

class WorldSynchronizationService {
    private val logger = LoggerFactory.getLogger(WorldSynchronizationService::class.java)
    private val playerUpdating = PlayerUpdating.getInstance()
    private val npcUpdating = NpcUpdating.getInstance()
    private val activePlayerBuffer = ArrayList<Client>(gameMaxPlayers + 1)
    private val stagedPlayerSynchronization = StagedPlayerSynchronizationService()
    private val stagedNpcSynchronization = StagedNpcSynchronizationService()
    private var tick = 0L

    fun run() {
        GameThreadContext.validateGameThread("world.synchronization")
        val startedNs = System.nanoTime()
        tick++
        val activePlayers = currentActivePlayers()
        val readyPlayers = activePlayers.filter(Client::isSynchronizationReady)
        val viewportIndex = ViewportIndex.build(activePlayers, VIEW_DISTANCE)
        val relevantNpcs: Collection<Npc> = viewportIndex?.relevantNpcs() ?: emptyList()
        val processedNpcs: Collection<Npc> = EntityProcessor.activeNpcsForSynchronization()
        val rootCache = RootSynchronizationCache()
        val cycle =
            SynchronizationCycle(
                tick = tick,
                rootCache = rootCache,
                viewportIndex = viewportIndex,
            )

        SynchronizationContext.setCurrent(cycle)
        try {
            measure(cycle, SynchronizationStage.SYNC_PLAYER_PREP) {
                buildPlayerRootCache(activePlayers, rootCache)
            }
            measure(cycle, SynchronizationStage.SYNC_NPC_PREP) {
                buildNpcRootCache(relevantNpcs, rootCache)
            }
            measure(cycle, SynchronizationStage.SYNC_PLAYER_ENCODE) {
                encodePlayers(readyPlayers)
            }
            measure(cycle, SynchronizationStage.SYNC_NPC_ENCODE) {
                encodeNpcs(activePlayers)
            }
            measure(cycle, SynchronizationStage.SYNC_FLUSH) {
                flushActivePlayers(readyPlayers)
            }
            measure(cycle, SynchronizationStage.SYNC_FLAG_CLEAR) {
                clearFlags(activePlayers, processedNpcs)
            }
        } finally {
            SynchronizationContext.clear()
            viewportIndex?.release()
        }
        OperationalTelemetry.recordPhaseMillis("sync.total", (System.nanoTime() - startedNs) / 1_000_000L)
    }

    private fun currentActivePlayers(): List<Client> {
        activePlayerBuffer.clear()
        for (player in PlayerRegistry.playersOnline.values) {
            if (player.isSynchronizationReady) {
                val channel = player.channel
                if (channel != null && channel.isActive) {
                    activePlayerBuffer += player
                }
            }
        }
        activePlayerBuffer.sortBy(Client::getSlot)
        // Safe to return the live buffer: it's only read within this same tick's run(),
        // and SyncWorker.forEachViewer is a synchronous submit-and-join barrier, so any
        // parallel fan-out over this list has completed before the next tick clears it.
        return activePlayerBuffer
    }

    private fun buildPlayerRootCache(activePlayers: List<Client>, rootCache: RootSynchronizationCache) {
        activePlayers.forEach { player ->
            if (!player.isSynchronizationReady) return@forEach
            try {
                rootCache.playerMovements.put(player, playerUpdating.buildLocalMovement(player), tick)
                rootCache.playerBlocks.put(player, PHASE_ADD_LOCAL, playerUpdating.buildSharedBlock(player, PHASE_ADD_LOCAL), tick)
                if (player.updateFlags.isUpdateRequired) {
                    rootCache.playerBlocks.put(player, PHASE_UPDATE_LOCAL, playerUpdating.buildSharedBlock(player, PHASE_UPDATE_LOCAL), tick)
                }
            } catch (throwable: Throwable) {
                OperationalTelemetry.incrementCounter("sync.player.subject_prep_failure")
                handleViewerSyncFailure("player-block-prep", player, throwable)
            }
        }
    }

    private fun buildNpcRootCache(activeNpcs: Collection<Npc>, rootCache: RootSynchronizationCache) {
        activeNpcs.forEach { npc ->
            rootCache.npcMovements.put(npc, npcUpdating.buildLocalMovement(npc), tick)
            if (npc.updateFlags.isUpdateRequired) {
                npcUpdating.buildSharedBlock(npc)?.let { block ->
                    rootCache.npcBlocks.put(npc, block, tick)
                }
            }
        }
    }

    private fun encodePlayers(readyPlayers: List<Client>) {
        // Per-viewer encode is independent by construction (PREP caches frozen, thread-local
        // scratch, viewer-own commit state, MPSC outbound enqueue) — see SyncWorker contract.
        SyncWorker.forEachViewer(readyPlayers) { player ->
            try {
                val result = stagedPlayerSynchronization.synchronize(player)
                if (!result.accepted) {
                    OperationalTelemetry.incrementCounter("player.disconnect.sync_outbound_rejected")
                    player.noteDisconnectReason("sync-outbound-rejected")
                    player.setSynchronizationReady(false)
                    player.disconnected = true
                    return@forEachViewer
                }
                SynchronizationContext.recordPlayerPacketBuilt(result.committedCount)
                SynchronizationContext.recordViewer(result.committedCount, player.localNpcSize)
                if (serverDebugMode) {
                    validateSyncInvariants(player)
                }
            } catch (throwable: Throwable) {
                OperationalTelemetry.incrementCounter("sync.player.viewer_encode_failure")
                handleViewerSyncFailure("player-sync-staged", player, throwable)
            }
        }
    }

    private fun validateSyncInvariants(player: Client) {
        try {
            PlayerSyncInvariantValidator.validateViewerLocals(player, player.previousSyncSnapshot)
            player.previousSyncSnapshot = PlayerSyncInvariantValidator.snapshot(player)
        } catch (violation: RuntimeException) {
            OperationalTelemetry.incrementCounter("sync.player.invariant_violation")
            logger.error("Player sync invariant violation for viewer slot={}", player.slot, violation)
        }
    }

    private fun encodeNpcs(activePlayers: List<Client>) {
        SyncWorker.forEachViewer(activePlayers) { player ->
            if (!player.isSynchronizationReady) return@forEachViewer
            try {
                val result = stagedNpcSynchronization.synchronize(player)
                if (!result.accepted) {
                    OperationalTelemetry.incrementCounter("player.disconnect.sync_outbound_rejected")
                    player.noteDisconnectReason("sync-outbound-rejected")
                    player.setSynchronizationReady(false)
                    player.disconnected = true
                    return@forEachViewer
                }
                SynchronizationContext.recordNpcPacketBuilt(result.committedCount)
                SynchronizationContext.recordViewer(player.playerListSize, result.committedCount)
            } catch (throwable: Throwable) {
                handleViewerSyncFailure("npc-sync", player, throwable)
            }
        }
    }

    private fun flushActivePlayers(readyPlayers: List<Client>) {
        val uiNanos = measureNanoTime { PlayerUiDeltaProcessor.process(readyPlayers) }
        val zoneStatsRef = arrayOfNulls<net.dodian.uber.game.engine.systems.zone.ZoneFlushStats>(1)
        val zoneNanos =
            measureNanoTime {
                zoneStatsRef[0] = ZoneUpdateBus.flush(readyPlayers)
            }
        var flushedPlayers = 0
        var flushedMessages = 0
        var flushedBytes = 0
        val netNanos =
            measureNanoTime {
                readyPlayers.forEach { player ->
                    try {
                        val flushStats = player.flushOutbound()
                        if (flushStats.flushedMessages() > 0) {
                            flushedPlayers++
                            flushedMessages += flushStats.flushedMessages()
                            flushedBytes += flushStats.flushedBytes()
                        }
                    } catch (throwable: Throwable) {
                        handleViewerSyncFailure("outbound-flush", player, throwable)
                    }
                }
            }
        val totalMs = (uiNanos + zoneNanos + netNanos) / 1_000_000L
        if (totalMs >= runtimePhaseWarnMs) {
            val zoneStats = zoneStatsRef[0] ?: net.dodian.uber.game.engine.systems.zone.ZoneFlushStats.EMPTY
            logger.warn(
                "SYNC_FLUSH detail: total={}ms ui={}ms zone={}ms net={}ms playersFlushed={} messages={} bytes={} zoneDeltas={} zoneCandidates={} zoneDeliveries={}",
                totalMs,
                uiNanos / 1_000_000L,
                zoneNanos / 1_000_000L,
                netNanos / 1_000_000L,
                flushedPlayers,
                flushedMessages,
                flushedBytes,
                zoneStats.deltas,
                zoneStats.candidateViewers,
                zoneStats.deliveries,
            )
        }
    }

    private fun clearFlags(activePlayers: List<Client>, relevantNpcs: Collection<Npc>) {
        relevantNpcs.forEach(Npc::clearUpdateFlags)
        activePlayers.forEach(Client::clearUpdateFlags)
    }

    private fun handleViewerSyncFailure(stage: String, player: Client, throwable: Throwable) {
        logger.error(
            "World sync viewer failure stage={} tick={} player={} dbId={} slot={} session={} ready={} pos={} locals={} movement=[{},{}] flags={}",
            stage,
            tick,
            player.playerName,
            player.dbId,
            player.slot,
            player.synchronizationSessionGeneration,
            player.isSynchronizationReady,
            player.position,
            player.playerListSize,
            player.primaryDirection,
            player.secondaryDirection,
            player.updateFlags,
            throwable,
        )
        player.noteDisconnectReason("sync-failure:$stage")
        OperationalTelemetry.incrementCounter("player.disconnect.sync_failure")
        player.setSynchronizationReady(false)
        player.disconnected = true
    }

    private fun measure(cycle: SynchronizationCycle, stage: SynchronizationStage, block: () -> Unit) {
        val elapsed = measureNanoTime(block)
        cycle.recordStage(stage, elapsed)
        val elapsedMs = elapsed / 1_000_000L
        OperationalTelemetry.recordPhaseMillis("sync.${stage.name.lowercase()}", elapsedMs)
        if (elapsedMs >= runtimePhaseWarnMs) {
            OperationalTelemetry.incrementCounter("sync.slow")
            when (stage) {
                SynchronizationStage.SYNC_PLAYER_ENCODE -> {
                    val built = cycle.playerPacketsBuilt
                    val avgLocals = if (built > 0) cycle.playerLocalScans.toDouble() / built.toDouble() else 0.0
                    logger.warn(
                        "Sync stage {} took {}ms viewersBuilt={} avgLocalPlayers={}",
                        stage,
                        elapsedMs,
                        built,
                        String.format("%.2f", avgLocals),
                    )
                }

                SynchronizationStage.SYNC_NPC_ENCODE -> {
                    val built = cycle.npcPacketsBuilt
                    val avgLocals = if (built > 0) cycle.npcLocalScans.toDouble() / built.toDouble() else 0.0
                    logger.warn(
                        "Sync stage {} took {}ms viewersBuilt={} avgLocalNpcs={}",
                        stage,
                        elapsedMs,
                        built,
                        String.format("%.2f", avgLocals),
                    )
                }

                else -> logger.warn("Sync stage {} took {}ms", stage, elapsedMs)
            }
        }
    }

    companion object {
        @JvmField
        val INSTANCE = WorldSynchronizationService()

        const val PHASE_ADD_LOCAL = "ADD_LOCAL"
        const val PHASE_UPDATE_LOCAL = "UPDATE_LOCAL"

        private const val VIEW_DISTANCE = 16
    }
}
