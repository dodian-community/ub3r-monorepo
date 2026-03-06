package net.dodian.jobs.impl;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.npc.NpcUpdating;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.entity.player.PlayerUpdating;
import net.dodian.uber.game.runtime.sync.WorldSynchronizationService;
import net.dodian.uber.game.runtime.zone.ZoneUpdateBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static net.dodian.utilities.DotEnvKt.getSynchronizationEnabled;

/**
 * Flushes outbound entity update packets after simulation/actions for the tick are complete.
 */
public class OutboundPacketProcessor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(OutboundPacketProcessor.class);
    private static final boolean DEBUG_ADDED_LOCAL_PLAYERS = false;
    private static final boolean DEBUG_NPC_MOVEMENT_WRITES = false;
    private final LegacyRunner legacyRunner = new LegacyRunner();

    @Override
    public void run() {
        if (getSynchronizationEnabled()) {
            WorldSynchronizationService.INSTANCE.run();
            return;
        }
        legacyRunner.run();
    }

    public static final class LegacyRunner implements Runnable {
        @Override
        public void run() {
            if (DEBUG_ADDED_LOCAL_PLAYERS) {
                PlayerUpdating.resetDebugAddedLocalCounter();
            }

            java.util.List<Client> activePlayers = PlayerHandler.snapshotActivePlayers();
            for (Client player : activePlayers) {
                updatePlayer(player);
            }

            activePlayers = PlayerHandler.snapshotActivePlayers();
            ZoneUpdateBus.flush(activePlayers);

            for (Client player : activePlayers) {
                player.flushOutbound();
            }

            for (Npc npc : Server.npcManager.getNpcs()) {
                if (npc != null) {
                    npc.clearUpdateFlags();
                }
            }
            for (Client player : activePlayers) {
                player.clearUpdateFlags();
            }

            if (DEBUG_ADDED_LOCAL_PLAYERS) {
                logger.info("addedLocalPlayers={}", PlayerUpdating.consumeDebugAddedLocalCounter());
            }
            if (DEBUG_NPC_MOVEMENT_WRITES) {
                logger.info("npcMovementWrites={}", NpcUpdating.consumeDebugMovementWriteCounter());
            }
        }

        private static void updatePlayer(Client player) {
            if (player.timeOutCounter.get() >= 84) {
                player.disconnected = true;
                player.println_debug("\nRemove non-responding " + player.getPlayerName() + " after 60 seconds of disconnect! ");
            }

            if (player.disconnected) {
                player.println_debug("\nRemove disconnected player " + player.getPlayerName());
                Server.playerHandler.removePlayer(player);
                player.disconnected = false;
                PlayerHandler.players[player.getSlot()] = null;
            } else {
                player.update();
            }
        }
    }
}
