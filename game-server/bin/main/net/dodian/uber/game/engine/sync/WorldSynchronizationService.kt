package net.dodian.uber.game.engine.sync

import io.netty.buffer.ByteBuf
import kotlin.system.measureNanoTime
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.npc.NpcUpdating
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.world.player.PlayerRegistry
import net.dodian.uber.game.model.entity.player.PlayerUpdating
import net.dodian.uber.game.netty.codec.ByteMessage
import net.dodian.uber.game.netty.codec.MessageType
import net.dodian.uber.game.engine.sync.cache.RootSynchronizationCache
import net.dodian.uber.game.engine.sync.npc.NpcChunkActivityIndex
import net.dodian.uber.game.engine.sync.npc.NpcSyncDecision
import net.dodian.uber.game.engine.sync.npc.RootNpcDeltaIndex
import net.dodian.uber.game.engine.sync.npc.ViewerNpcSyncState
import net.dodian.uber.game.engine.sync.player.PlayerChunkActivityIndex
import net.dodian.uber.game.engine.sync.player.PlayerSyncDecision
import net.dodian.uber.game.engine.sync.player.PlayerSyncRevisionIndex
import net.dodian.uber.game.engine.sync.player.ViewerPlayerSyncState
import net.dodian.uber.game.engine.sync.playerinfo.RootPlayerInfoService
import net.dodian.uber.game.content.ui.PlayerUiDeltaProcessor
import net.dodian.uber.game.engine.sync.viewport.ViewportIndex
import net.dodian.uber.game.systems.zone.ZoneUpdateBus
import net.dodian.uber.game.engine.config.runtimePhaseWarnMs
import org.slf4j.LoggerFactory

class WorldSynchronizationService {
    private val logger = LoggerFactory.getLogger(WorldSynchronizationService::class.java)
    private val playerUpdating = PlayerUpdating.getInstance()
    private val npcUpdating = NpcUpdating.getInstance()
    private val playerRevisionIndex = PlayerSyncRevisionIndex()
    private val npcRevisionIndex = RootNpcDeltaIndex()
    private val rootPlayerInfoService = RootPlayerInfoService.INSTANCE
    private val sharedPlayerActivityIndex = PlayerChunkActivityIndex()
    private val sharedNpcActivityIndex = NpcChunkActivityIndex()
    private val activePlayerBuffer = ArrayList<Client>(2048)
    private var tick = 0L

    fun run() {
        tick++
        val activePlayers = currentActivePlayers()
        val viewportIndex = ViewportIndex.build(activePlayers, VIEW_DISTANCE)
        val relevantNpcs: Collection<Npc> = viewportIndex?.relevantNpcs() ?: emptyList()
        val rootCache = RootSynchronizationCache()
        val playerActivityIndex = sharedPlayerActivityIndex.apply { clear() }
        val npcActivityIndex = sharedNpcActivityIndex.apply { clear() }
        playerRevisionIndex.rebuild(activePlayers, tick, playerActivityIndex)
        npcRevisionIndex.rebuild(relevantNpcs, tick, npcActivityIndex)
        val cycle =
            SynchronizationCycle(
                tick = tick,
                rootCache = rootCache,
                viewportIndex = viewportIndex,
                playerRevisionIndex = playerRevisionIndex,
                playerActivityIndex = playerActivityIndex,
                npcRevisionIndex = npcRevisionIndex,
                npcActivityIndex = npcActivityIndex,
            )

        measure(cycle, SynchronizationStage.SYNC_PLAYER_PREP) {
            buildPlayerRootCache(activePlayers, rootCache)
        }
        measure(cycle, SynchronizationStage.SYNC_NPC_PREP) {
            buildNpcRootCache(relevantNpcs, rootCache)
        }

        SynchronizationContext.setCurrent(cycle)
        try {
            measure(cycle, SynchronizationStage.SYNC_PLAYER_ENCODE) {
                encodePlayers(activePlayers)
            }
            measure(cycle, SynchronizationStage.SYNC_NPC_ENCODE) {
                encodeNpcs(activePlayers)
            }
            measure(cycle, SynchronizationStage.SYNC_FLUSH) {
                flushActivePlayers(activePlayers)
            }
            measure(cycle, SynchronizationStage.SYNC_FLAG_CLEAR) {
                clearFlags(activePlayers, relevantNpcs)
            }
        } finally {
            SynchronizationContext.clear()
        }
    }

    private fun currentActivePlayers(): List<Client> {
        activePlayerBuffer.clear()
        for (player in PlayerRegistry.playersOnline.values) {
            if (player.isActive && !player.disconnected) {
                val channel = player.channel
                if (channel != null && channel.isActive) {
                    activePlayerBuffer += player
                }
            }
        }
        return activePlayerBuffer
    }

    private fun buildPlayerRootCache(activePlayers: List<Client>, rootCache: RootSynchronizationCache) {
        activePlayers.forEach { player ->
            rootCache.playerBlocks.put(player, PHASE_ADD_LOCAL, playerUpdating.buildSharedBlock(player, PHASE_ADD_LOCAL))
            if (player.updateFlags.isUpdateRequired) {
                rootCache.playerBlocks.put(player, PHASE_UPDATE_LOCAL, playerUpdating.buildSharedBlock(player, PHASE_UPDATE_LOCAL))
            }
        }
    }

    private fun buildNpcRootCache(activeNpcs: Collection<Npc>, rootCache: RootSynchronizationCache) {
        activeNpcs.forEach { npc ->
            if (npc.updateFlags.isUpdateRequired) {
                rootCache.npcBlocks.put(npc, npcUpdating.buildSharedBlock(npc))
            }
        }
    }

    private fun encodePlayers(activePlayers: List<Client>) {
        rootPlayerInfoService.sync(activePlayers)
    }

    private fun encodeNpcs(activePlayers: List<Client>) {
        activePlayers.forEach { player ->
            try {
                val state = SynchronizationContext.getViewerNpcSyncState(player)
                val chunkStamp = SynchronizationContext.getNpcChunkActivityStamp(player)
                val localActivityStamp = SynchronizationContext.getNpcLocalActivityStamp(player)
                val membershipRevision = player.localNpcMembershipRevision
                val decision = shouldSkipNpcSync(player, state, chunkStamp, localActivityStamp, membershipRevision)
                if (decision == NpcSyncDecision.SKIP) {
                    SynchronizationContext.recordNpcPacketSkipped(player.localNpcs.size)
                    SynchronizationContext.recordViewer(player.playerListSize, player.localNpcs.size)
                    updateNpcViewerSyncState(player, chunkStamp, localActivityStamp, membershipRevision)
                    return@forEach
                }
                player.sendNpcSynchronization()
                SynchronizationContext.recordNpcPacketBuilt(player.localNpcs.size)
                SynchronizationContext.recordViewer(player.playerListSize, player.localNpcs.size)
                updateNpcViewerSyncState(player, chunkStamp, localActivityStamp, membershipRevision)
            } catch (throwable: Throwable) {
                handleViewerSyncFailure("npc-sync", player, throwable)
            }
        }
    }

    private fun flushActivePlayers(activePlayers: List<Client>) {
        val uiNanos = measureNanoTime { PlayerUiDeltaProcessor.process(activePlayers) }
        val zoneStatsRef = arrayOfNulls<net.dodian.uber.game.systems.zone.ZoneFlushStats>(1)
        val zoneNanos =
            measureNanoTime {
                zoneStatsRef[0] = ZoneUpdateBus.flush(activePlayers)
            }
        var flushedPlayers = 0
        var flushedMessages = 0
        var flushedBytes = 0
        val netNanos =
            measureNanoTime {
                activePlayers.forEach { player ->
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
            val zoneStats = zoneStatsRef[0] ?: net.dodian.uber.game.systems.zone.ZoneFlushStats.EMPTY
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
            "World sync viewer failure stage={} player={} slot={} pos={}",
            stage,
            player.playerName,
            player.slot,
            player.position,
            throwable,
        )
        player.disconnected = true
    }

    private fun shouldSkipNpcSync(
        player: Client,
        state: ViewerNpcSyncState?,
        chunkStamp: Long,
        localActivityStamp: Long,
        membershipRevision: Long,
    ): NpcSyncDecision {
        state ?: run {
            SynchronizationContext.recordNpcBuildNoState()
            return NpcSyncDecision.BUILD
        }
        val mapChanged =
            state.lastKnownMapRegionX != Int.MIN_VALUE &&
                (state.lastKnownMapRegionX != player.mapRegionX || state.lastKnownMapRegionY != player.mapRegionY)
        val planeChanged = state.lastKnownPlane != Int.MIN_VALUE && state.lastKnownPlane != player.position.z
        if (mapChanged || planeChanged || player.didTeleport() || player.didMapRegionChange()) {
            SynchronizationContext.recordNpcBuildMapRegionOrTeleport()
            return NpcSyncDecision.BUILD
        }
        if (state.lastKnownLocalNpcCount != player.localNpcs.size) {
            SynchronizationContext.recordNpcBuildLocalCountChanged()
            return NpcSyncDecision.BUILD
        }
        val snapshot = SynchronizationContext.getViewportSnapshot(player)
        if (player.localNpcs.isEmpty() && snapshot != null && snapshot.npcs.isNotEmpty()) {
            SynchronizationContext.recordNpcBuildPendingViewport()
            return NpcSyncDecision.BUILD
        }
        return if (
            state.lastChunkActivityStamp == chunkStamp &&
            state.lastLocalNpcActivityStamp == localActivityStamp &&
            state.lastLocalNpcMembershipRevision == membershipRevision
        ) {
            NpcSyncDecision.SKIP
        } else {
            if (state.lastChunkActivityStamp != chunkStamp) {
                SynchronizationContext.recordNpcBuildChunkActivityChanged()
            }
            if (state.lastLocalNpcActivityStamp != localActivityStamp) {
                SynchronizationContext.recordNpcBuildLocalActivityChanged()
            }
            if (state.lastLocalNpcMembershipRevision != membershipRevision) {
                SynchronizationContext.recordNpcBuildLocalMembershipChanged()
            }
            NpcSyncDecision.BUILD
        }
    }

    private fun measure(cycle: SynchronizationCycle, stage: SynchronizationStage, block: () -> Unit) {
        val elapsed = measureNanoTime(block)
        cycle.recordStage(stage, elapsed)
        val elapsedMs = elapsed / 1_000_000L
        if (elapsedMs >= runtimePhaseWarnMs) {
            when (stage) {
                SynchronizationStage.SYNC_PLAYER_ENCODE -> {
                    val built = cycle.playerPacketsBuilt + cycle.playerPacketsTemplated
                    val idleTemplated = cycle.playerPacketsIdleTemplated
                    val skipped = cycle.playerPacketsSkipped
                    val total = built + idleTemplated + skipped
                    val localCoverage = cycle.playerLocalScans + cycle.playerLocalsSkipped + cycle.playerTemplatedLocalCoverage
                    val avgLocals = if (total > 0) localCoverage.toDouble() / total.toDouble() else 0.0
                    logger.warn(
                        "Sync stage {} took {}ms viewersBuilt={} viewersIdleTemplated={} viewersSkipped={} teleportReinserts={} avgLocalPlayers={}",
                        stage,
                        elapsedMs,
                        built,
                        idleTemplated,
                        skipped,
                        cycle.playerTeleportReinsertCount,
                        String.format("%.2f", avgLocals),
                    )
                }

                SynchronizationStage.SYNC_NPC_ENCODE -> {
                    val built = cycle.npcPacketsBuilt
                    val skipped = cycle.npcPacketsSkipped
                    val total = built + skipped
                    val localCoverage = cycle.npcLocalScans + cycle.npcLocalsSkipped
                    val avgLocals = if (total > 0) localCoverage.toDouble() / total.toDouble() else 0.0
                    logger.warn(
                        "Sync stage {} took {}ms viewersBuilt={} viewersSkipped={} avgLocalNpcs={} buildReasons=[noState={}, mapOrTeleport={}, localCount={}, pendingViewport={}, chunkStamp={}, localActivity={}, membership={}]",
                        stage,
                        elapsedMs,
                        built,
                        skipped,
                        String.format("%.2f", avgLocals),
                        cycle.npcBuildNoStateCount,
                        cycle.npcBuildMapRegionOrTeleportCount,
                        cycle.npcBuildLocalCountChangedCount,
                        cycle.npcBuildPendingViewportCount,
                        cycle.npcBuildChunkActivityChangedCount,
                        cycle.npcBuildLocalActivityChangedCount,
                        cycle.npcBuildLocalMembershipChangedCount,
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

    private fun updateViewerSyncState(player: Client) {
        val state: ViewerPlayerSyncState = playerRevisionIndex.viewerState(player)
        val chunkStamp = SynchronizationContext.getPlayerChunkActivityStamp(player)
        val localActivityStamp = SynchronizationContext.getPlayerLocalActivityStamp(player)
        val membershipRevision = player.localPlayerMembershipRevision
        state.lastPlayerSyncTick = tick
        state.lastSelfMovementRevision = playerRevisionIndex.movementRevision(player)
        state.lastSelfBlockRevision = playerRevisionIndex.blockRevision(player)
        state.lastViewportRevision = chunkStamp
        state.lastKnownLocalCount = player.playerListSize
        state.lastKnownMapRegionX = player.mapRegionX
        state.lastKnownMapRegionY = player.mapRegionY
        state.lastKnownPlane = player.position.z
        state.lastKnownTeleportState = player.didTeleport()
        state.lastChunkActivityStamp = chunkStamp
        state.lastLocalActivityStamp = localActivityStamp
        state.lastLocalMembershipRevision = membershipRevision
    }

    private fun sendPlayerTemplate(player: Client, payload: ByteArray) {
        val pooledBuffer: ByteBuf = ByteMessage.pooledBuffer(payload.size.coerceAtLeast(64))
        val message = ByteMessage.message(81, MessageType.VAR_SHORT, pooledBuffer)
        message.putBytes(payload)
        player.send(message)
    }

    private fun updateNpcViewerSyncState(
        player: Client,
        chunkStamp: Long,
        localActivityStamp: Long,
        membershipRevision: Long,
    ) {
        val state: ViewerNpcSyncState = npcRevisionIndex.viewerState(player)
        state.lastNpcSyncTick = tick
        state.lastNpcViewportRevision = chunkStamp
        state.lastKnownLocalNpcCount = player.localNpcs.size
        state.lastKnownMapRegionX = player.mapRegionX
        state.lastKnownMapRegionY = player.mapRegionY
        state.lastKnownPlane = player.position.z
        state.lastChunkActivityStamp = chunkStamp
        state.lastLocalNpcActivityStamp = localActivityStamp
        state.lastLocalNpcMembershipRevision = membershipRevision
    }
}
