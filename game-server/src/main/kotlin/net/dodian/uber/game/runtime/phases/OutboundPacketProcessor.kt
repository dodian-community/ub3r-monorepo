package net.dodian.uber.game.runtime.phases

import net.dodian.uber.game.Constants
import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.npc.NpcUpdating
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.model.entity.player.PlayerUpdating
import net.dodian.uber.game.runtime.sync.WorldSynchronizationService
import net.dodian.uber.game.runtime.zone.ZoneUpdateBus
import net.dodian.utilities.synchronizationEnabled
import org.slf4j.LoggerFactory

open class OutboundPacketProcessor(
    private val syncEnabledProvider: () -> Boolean = { synchronizationEnabled },
    private val syncRunner: (() -> Unit)? = null,
    private val activePlayersSnapshot: () -> List<Client> = { PlayerHandler.snapshotActivePlayers() },
    private val zoneFlush: (List<Client>) -> Unit = { players -> ZoneUpdateBus.flush(players) },
    private val npcProvider: () -> Iterable<Npc?> = { Server.npcManager.getNpcs().asIterable() },
    private val npcFlagClearer: (() -> Unit)? = null,
    private val removePlayer: (Client) -> Unit = { player -> Server.playerHandler.removePlayer(player) },
) : Runnable {
    override fun run() {
        if (syncEnabledProvider()) {
            runSynchronization()
            return
        }
        runLegacyOutbound()
    }

    protected open fun runSynchronization() {
        val runner = syncRunner
        if (runner != null) {
            runner()
            return
        }
        WorldSynchronizationService.INSTANCE.run()
    }

    private fun runLegacyOutbound() {
        if (DEBUG_ADDED_LOCAL_PLAYERS) {
            PlayerUpdating.resetDebugAddedLocalCounter()
        }

        var activePlayers = activePlayersSnapshot()
        for (player in activePlayers) {
            updatePlayer(player)
        }

        // Refresh active players after updates/removals so zone flush and outbound drain only touch
        // clients that survived this outbound phase.
        activePlayers = activePlayersSnapshot()
        zoneFlush(activePlayers)

        for (player in activePlayers) {
            player.flushOutbound()
        }

        clearNpcFlags()
        clearPlayerFlags(activePlayers)

        if (DEBUG_ADDED_LOCAL_PLAYERS) {
            logger.info("addedLocalPlayers={}", PlayerUpdating.consumeDebugAddedLocalCounter())
        }
        if (DEBUG_NPC_MOVEMENT_WRITES) {
            logger.info("npcMovementWrites={}", NpcUpdating.consumeDebugMovementWriteCounter())
        }
    }

    private fun updatePlayer(player: Client) {
        if (player.timeOutCounter.get() >= PLAYER_TIMEOUT_THRESHOLD) {
            player.disconnected = true
            player.println_debug("\nRemove non-responding ${player.playerName} after 60 seconds of disconnect! ")
        }

        if (player.disconnected) {
            player.println_debug("\nRemove disconnected player ${player.playerName}")
            removePlayer(player)
            player.disconnected = false
            PlayerHandler.players[player.slot] = null
            return
        }

        player.update()
    }

    private fun clearNpcFlags() {
        val clearer = npcFlagClearer
        if (clearer != null) {
            clearer()
            return
        }
        for (npc in npcProvider()) {
            npc?.clearUpdateFlags()
        }
    }

    private fun clearPlayerFlags(activePlayers: List<Client>) {
        for (player in activePlayers) {
            player.clearUpdateFlags()
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(OutboundPacketProcessor::class.java)
        private const val DEBUG_ADDED_LOCAL_PLAYERS = false
        private const val DEBUG_NPC_MOVEMENT_WRITES = false
        private const val PLAYER_TIMEOUT_THRESHOLD = 84
    }
}
