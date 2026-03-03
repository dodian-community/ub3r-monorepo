package net.dodian.uber.game.runtime.sync

import io.netty.buffer.ByteBuf
import kotlin.system.measureNanoTime
import java.util.IdentityHashMap
import net.dodian.uber.game.Constants
import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.npc.NpcUpdating
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.model.entity.player.PlayerUpdating
import net.dodian.uber.game.netty.codec.ByteMessage
import net.dodian.uber.game.netty.codec.MessageType
import net.dodian.uber.game.runtime.sync.cache.RootSynchronizationCache
import net.dodian.uber.game.runtime.sync.metrics.SynchronizationMetrics
import net.dodian.uber.game.runtime.sync.npc.NpcChunkActivityIndex
import net.dodian.uber.game.runtime.sync.npc.NpcSyncDecision
import net.dodian.uber.game.runtime.sync.npc.RootNpcDeltaIndex
import net.dodian.uber.game.runtime.sync.npc.ViewerNpcSyncState
import net.dodian.uber.game.runtime.sync.player.PlayerChunkActivityIndex
import net.dodian.uber.game.runtime.sync.player.PlayerSyncDecision
import net.dodian.uber.game.runtime.sync.player.PlayerSyncRevisionIndex
import net.dodian.uber.game.runtime.sync.player.ViewerPlayerSyncState
import net.dodian.uber.game.runtime.sync.player.root.RootPlayerInfoService
import net.dodian.uber.game.runtime.ui.PlayerUiDeltaProcessor
import net.dodian.uber.game.runtime.sync.viewport.ViewportIndex
import net.dodian.uber.game.runtime.zone.ZoneUpdateBus
import net.dodian.utilities.runtimePhaseWarnMs
import net.dodian.utilities.syncAppearanceCacheEnabled
import net.dodian.utilities.syncMetricsLogIntervalTicks
import net.dodian.utilities.syncMetricsVerboseEnabled
import net.dodian.utilities.syncNpcActivityIndexEnabled
import net.dodian.utilities.syncPlayerActivityIndexEnabled
import net.dodian.utilities.playerSynchronizationEnabled
import net.dodian.utilities.syncPlayerTemplateCacheEnabled
import net.dodian.utilities.syncRootBlockCacheEnabled
import net.dodian.utilities.syncScratchBufferReuseEnabled
import net.dodian.utilities.syncSkipEmptyPlayerPacketEnabled
import net.dodian.utilities.syncSkipEmptyNpcPacketEnabled
import org.slf4j.LoggerFactory

class WorldSynchronizationService {
    private val logger = LoggerFactory.getLogger(WorldSynchronizationService::class.java)
    private val playerUpdating = PlayerUpdating.getInstance()
    private val npcUpdating = NpcUpdating.getInstance()
    private val metrics = SynchronizationMetrics(syncMetricsLogIntervalTicks.coerceAtLeast(1))
    private val playerRevisionIndex = PlayerSyncRevisionIndex()
    private val npcRevisionIndex = RootNpcDeltaIndex()
    private val rootPlayerInfoService = RootPlayerInfoService.INSTANCE
    private var tick = 0L

    fun run() {
        tick++
        val activePlayers = currentActivePlayers()
        val viewportIndex = ViewportIndex.build(activePlayers, VIEW_DISTANCE)
        val relevantNpcs: Collection<Npc> =
            if (viewportIndex != null) {
                collectRelevantNpcs(activePlayers, viewportIndex)
            } else {
                currentActiveNpcs()
            }
        val rootCache = RootSynchronizationCache()
        val playerActivityIndex = if (syncPlayerActivityIndexEnabled) PlayerChunkActivityIndex() else null
        val npcActivityIndex = if (syncNpcActivityIndexEnabled) NpcChunkActivityIndex() else null
        if (playerActivityIndex != null) {
            playerRevisionIndex.rebuild(activePlayers, tick, playerActivityIndex)
        }
        if (npcActivityIndex != null) {
            npcRevisionIndex.rebuild(relevantNpcs, tick, npcActivityIndex)
        }
        val cycle =
            SynchronizationCycle(
                tick = tick,
                rootCache = rootCache,
                viewportIndex = viewportIndex,
                playerRevisionIndex = if (syncPlayerActivityIndexEnabled) playerRevisionIndex else null,
                playerActivityIndex = playerActivityIndex,
                npcRevisionIndex = if (syncNpcActivityIndexEnabled) npcRevisionIndex else null,
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
                flushActivePlayers()
            }
            measure(cycle, SynchronizationStage.SYNC_FLAG_CLEAR) {
                clearFlags()
            }
        } finally {
            SynchronizationContext.clear()
            if (syncMetricsVerboseEnabled) {
                measure(cycle, SynchronizationStage.SYNC_METRICS_LOG) {
                    metrics.record(cycle, PlayerHandler.getPlayerCount(), logger)
                }
            }
        }
    }

    private fun currentActivePlayers(): List<Client> {
        val players = ArrayList<Client>(PlayerHandler.getPlayerCount().coerceAtLeast(1))
        for (i in 0 until Constants.maxPlayers) {
            val player = PlayerHandler.players[i] as? Client ?: continue
            if (player.isActive) {
                players += player
            }
        }
        return players
    }

    private fun currentActiveNpcs(): List<Npc> {
        val npcs = ArrayList<Npc>()
        for (npc in Server.npcManager.getNpcs()) {
            if (npc != null) {
                npcs += npc
            }
        }
        return npcs
    }

    private fun collectRelevantNpcs(viewers: List<Client>, viewportIndex: ViewportIndex): List<Npc> {
        val seen = IdentityHashMap<Npc, Boolean>()
        val out = ArrayList<Npc>()
        viewers.forEach { viewer ->
            val snapshot = viewportIndex.snapshotFor(viewer) ?: return@forEach
            snapshot.npcs.forEach { npc ->
                if (seen.put(npc, true) == null) {
                    out += npc
                }
            }
        }
        return out
    }

    private fun buildPlayerRootCache(activePlayers: List<Client>, rootCache: RootSynchronizationCache) {
        if (!syncRootBlockCacheEnabled) {
            return
        }
        activePlayers.forEach { player ->
            rootCache.playerBlocks.put(player, PHASE_ADD_LOCAL, playerUpdating.buildSharedBlock(player, PHASE_ADD_LOCAL))
            if (player.updateFlags.isUpdateRequired) {
                rootCache.playerBlocks.put(player, PHASE_UPDATE_LOCAL, playerUpdating.buildSharedBlock(player, PHASE_UPDATE_LOCAL))
            }
        }
    }

    private fun buildNpcRootCache(activeNpcs: Collection<Npc>, rootCache: RootSynchronizationCache) {
        if (!syncRootBlockCacheEnabled) {
            return
        }
        activeNpcs.forEach { npc ->
            if (npc.updateFlags.isUpdateRequired) {
                rootCache.npcBlocks.put(npc, npcUpdating.buildSharedBlock(npc))
            }
        }
    }

    private fun encodePlayers(activePlayers: List<Client>) {
        if (playerSynchronizationEnabled) {
            rootPlayerInfoService.sync(activePlayers)
            return
        }
        activePlayers.forEach { player ->
            if (player.timeOutCounter >= 84) {
                player.disconnected = true
                player.println_debug("\nRemove non-responding " + player.playerName + " after 60 seconds of disconnect! ")
            }

            if (player.disconnected) {
                player.println_debug("\nRemove disconnected player " + player.playerName)
                Server.playerHandler.removePlayer(player)
                player.disconnected = false
                PlayerHandler.players[player.slot] = null
                return@forEach
            }

            val decision = playerUpdating.shouldSkipPlayerSync(player)
            if (decision == PlayerSyncDecision.SKIP) {
                if (syncSkipEmptyPlayerPacketEnabled) {
                    SynchronizationContext.recordPlayerPacketSkipped(player.playerListSize)
                    updateViewerSyncState(player)
                    return@forEach
                }
                if (syncPlayerTemplateCacheEnabled) {
                    val key = playerUpdating.buildPlayerSyncTemplateKey(player)
                    val template =
                        SynchronizationContext.getPlayerTemplate(key)
                            ?: playerUpdating.buildPlayerSyncTemplate(player).also {
                                SynchronizationContext.putPlayerTemplate(key, it)
                            }
                    sendPlayerTemplate(player, template.payload)
                    SynchronizationContext.recordPlayerPacketTemplated(player.playerListSize)
                    updateViewerSyncState(player)
                    return@forEach
                }
            }
            player.sendPlayerSynchronization()
            updateViewerSyncState(player)
        }
    }

    private fun encodeNpcs(activePlayers: List<Client>) {
        val trackActivityStamps = syncSkipEmptyNpcPacketEnabled && syncNpcActivityIndexEnabled
        activePlayers.forEach { player ->
            val state = if (trackActivityStamps) SynchronizationContext.getViewerNpcSyncState(player) else null
            val chunkStamp = if (trackActivityStamps) SynchronizationContext.getNpcChunkActivityStamp(player) else 0L
            val localStamp = if (trackActivityStamps) SynchronizationContext.getNpcLocalActivityStamp(player) else 0L
            val decision =
                if (trackActivityStamps) {
                    shouldSkipNpcSync(player, state, chunkStamp, localStamp)
                } else {
                    NpcSyncDecision.BUILD
                }
            if (decision == NpcSyncDecision.SKIP) {
                SynchronizationContext.recordNpcPacketSkipped(player.localNpcs.size)
                SynchronizationContext.recordViewer(player.playerListSize, player.localNpcs.size)
                updateNpcViewerSyncState(player, chunkStamp, localStamp)
                return@forEach
            }
            player.sendNpcSynchronization()
            SynchronizationContext.recordNpcPacketBuilt(player.localNpcs.size)
            SynchronizationContext.recordViewer(player.playerListSize, player.localNpcs.size)
            updateNpcViewerSyncState(player, chunkStamp, localStamp)
        }
    }

    private fun flushActivePlayers() {
        val activePlayers = currentActivePlayers()
        PlayerUiDeltaProcessor.process(activePlayers)
        ZoneUpdateBus.flush(activePlayers)
        activePlayers.forEach { player ->
            player.flushOutbound()
        }
    }

    private fun clearFlags() {
        for (npc in Server.npcManager.getNpcs()) {
            npc?.clearUpdateFlags()
        }
        for (i in 0 until Constants.maxPlayers) {
            val player = PlayerHandler.players[i] as? Client ?: continue
            if (player.isActive) {
                player.clearUpdateFlags()
            }
        }
    }

    private fun shouldSkipNpcSync(
        player: Client,
        state: ViewerNpcSyncState?,
        chunkStamp: Long,
        localStamp: Long,
    ): NpcSyncDecision {
        state ?: return NpcSyncDecision.BUILD
        val mapChanged =
            state.lastKnownMapRegionX != Int.MIN_VALUE &&
                (state.lastKnownMapRegionX != player.mapRegionX || state.lastKnownMapRegionY != player.mapRegionY)
        val planeChanged = state.lastKnownPlane != Int.MIN_VALUE && state.lastKnownPlane != player.position.z
        if (mapChanged || planeChanged || player.didTeleport() || player.didMapRegionChange()) {
            return NpcSyncDecision.BUILD
        }
        if (state.lastKnownLocalNpcCount != player.localNpcs.size) {
            return NpcSyncDecision.BUILD
        }
        val snapshot = SynchronizationContext.getViewportSnapshot(player)
        if (player.localNpcs.isEmpty() && snapshot != null && snapshot.npcs.isNotEmpty()) {
            return NpcSyncDecision.BUILD
        }
        return if (
            state.lastChunkActivityStamp == chunkStamp &&
            state.lastLocalNpcActivityStamp == localStamp
        ) {
            NpcSyncDecision.SKIP
        } else {
            NpcSyncDecision.BUILD
        }
    }

    private fun measure(cycle: SynchronizationCycle, stage: SynchronizationStage, block: () -> Unit) {
        val elapsed = measureNanoTime(block)
        cycle.recordStage(stage, elapsed)
        val elapsedMs = elapsed / 1_000_000L
        if (elapsedMs >= runtimePhaseWarnMs) {
            logger.warn("Sync stage {} took {}ms", stage, elapsedMs)
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
        val trackActivityStamps = syncPlayerActivityIndexEnabled && syncSkipEmptyPlayerPacketEnabled
        val chunkStamp = if (trackActivityStamps) SynchronizationContext.getPlayerChunkActivityStamp(player) else 0L
        val localStamp = if (trackActivityStamps) SynchronizationContext.getPlayerLocalActivityStamp(player) else 0L
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
        state.lastLocalActivityStamp = localStamp
    }

    private fun sendPlayerTemplate(player: Client, payload: ByteArray) {
        val pooledBuffer: ByteBuf = ByteMessage.pooledBuffer(payload.size.coerceAtLeast(64))
        val message = ByteMessage.message(81, MessageType.VAR_SHORT, pooledBuffer)
        message.putBytes(payload)
        player.send(message)
    }

    private fun updateNpcViewerSyncState(player: Client, chunkStamp: Long, localStamp: Long) {
        val state: ViewerNpcSyncState = npcRevisionIndex.viewerState(player)
        state.lastNpcSyncTick = tick
        state.lastNpcViewportRevision = chunkStamp
        state.lastKnownLocalNpcCount = player.localNpcs.size
        state.lastKnownMapRegionX = player.mapRegionX
        state.lastKnownMapRegionY = player.mapRegionY
        state.lastKnownPlane = player.position.z
        state.lastChunkActivityStamp = chunkStamp
        state.lastLocalNpcActivityStamp = localStamp
    }
}
