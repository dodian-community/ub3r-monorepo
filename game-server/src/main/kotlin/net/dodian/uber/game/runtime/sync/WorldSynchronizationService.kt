package net.dodian.uber.game.runtime.sync

import kotlin.system.measureNanoTime
import net.dodian.uber.game.Constants
import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.npc.NpcUpdating
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.model.entity.player.PlayerUpdating
import net.dodian.uber.game.runtime.sync.cache.RootSynchronizationCache
import net.dodian.uber.game.runtime.sync.metrics.SynchronizationMetrics
import net.dodian.uber.game.runtime.sync.viewport.ViewportIndex
import net.dodian.utilities.runtimePhaseWarnMs
import net.dodian.utilities.syncMetricsLogIntervalTicks
import net.dodian.utilities.syncMetricsVerboseEnabled
import net.dodian.utilities.syncRootBlockCacheEnabled
import net.dodian.utilities.syncSkipEmptyNpcPacketEnabled
import net.dodian.utilities.syncViewportSnapshotEnabled
import org.slf4j.LoggerFactory

class WorldSynchronizationService {
    private val logger = LoggerFactory.getLogger(WorldSynchronizationService::class.java)
    private val playerUpdating = PlayerUpdating.getInstance()
    private val npcUpdating = NpcUpdating.getInstance()
    private val metrics = SynchronizationMetrics(syncMetricsLogIntervalTicks.coerceAtLeast(1))
    private var tick = 0L

    fun run() {
        tick++
        val activePlayers = currentActivePlayers()
        val rootCache = RootSynchronizationCache()
        val cycle =
            SynchronizationCycle(
                tick = tick,
                rootCache = rootCache,
                viewportIndex = if (syncViewportSnapshotEnabled) ViewportIndex.build(activePlayers, VIEW_DISTANCE) else null,
            )

        measure(cycle, SynchronizationStage.SYNC_PLAYER_PREP) {
            buildPlayerRootCache(activePlayers, rootCache)
        }
        measure(cycle, SynchronizationStage.SYNC_NPC_PREP) {
            buildNpcRootCache(rootCache)
        }

        SynchronizationContext.setCurrent(cycle)
        try {
            measure(cycle, SynchronizationStage.SYNC_PLAYER_ENCODE) {
                encodePlayers()
            }
            measure(cycle, SynchronizationStage.SYNC_NPC_ENCODE) {
                encodeNpcs()
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
                metrics.record(cycle, PlayerHandler.getPlayerCount(), logger)
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

    private fun buildPlayerRootCache(activePlayers: List<Client>, rootCache: RootSynchronizationCache) {
        if (!syncRootBlockCacheEnabled) {
            return
        }
        activePlayers.forEach { player ->
            rootCache.movementCache.freezePlayer(player)
            rootCache.playerBlocks.put(player, PHASE_ADD_LOCAL, playerUpdating.buildSharedBlock(player, PHASE_ADD_LOCAL))
            if (player.updateFlags.isUpdateRequired) {
                rootCache.playerBlocks.put(player, PHASE_UPDATE_LOCAL, playerUpdating.buildSharedBlock(player, PHASE_UPDATE_LOCAL))
            }
        }
    }

    private fun buildNpcRootCache(rootCache: RootSynchronizationCache) {
        for (npc in Server.npcManager.getNpcs()) {
            if (npc == null) {
                continue
            }
            rootCache.movementCache.freezeNpc(npc)
            if (!syncRootBlockCacheEnabled || !npc.updateFlags.isUpdateRequired) {
                continue
            }
            rootCache.npcBlocks.put(npc, npcUpdating.buildSharedBlock(npc))
        }
    }

    private fun encodePlayers() {
        for (i in 0 until Constants.maxPlayers) {
            val player = PlayerHandler.players[i] as? Client ?: continue
            if (!player.isActive) {
                continue
            }
            if (player.timeOutCounter >= 84) {
                player.disconnected = true
                player.println_debug("\nRemove non-responding " + player.playerName + " after 60 seconds of disconnect! ")
            }

            if (player.disconnected) {
                player.println_debug("\nRemove disconnected player " + player.playerName)
                Server.playerHandler.removePlayer(player)
                player.disconnected = false
                PlayerHandler.players[i] = null
                continue
            }
            player.sendPlayerSynchronization()
        }
    }

    private fun encodeNpcs() {
        for (i in 0 until Constants.maxPlayers) {
            val player = PlayerHandler.players[i] as? Client ?: continue
            if (!player.isActive) {
                continue
            }
            if (shouldSkipNpcSync(player)) {
                SynchronizationContext.recordViewer(player.playerListSize, player.localNpcs.size)
                continue
            }
            player.sendNpcSynchronization()
            SynchronizationContext.recordViewer(player.playerListSize, player.localNpcs.size)
        }
    }

    private fun flushActivePlayers() {
        for (i in 0 until Constants.maxPlayers) {
            val player = PlayerHandler.players[i] as? Client ?: continue
            if (player.isActive) {
                player.flushOutbound()
            }
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

    private fun shouldSkipNpcSync(player: Client): Boolean {
        if (!syncSkipEmptyNpcPacketEnabled || player.localNpcs.isNotEmpty()) {
            return false
        }
        val snapshot = SynchronizationContext.getViewportSnapshot(player) ?: return false
        return snapshot.npcs.isEmpty()
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
}
