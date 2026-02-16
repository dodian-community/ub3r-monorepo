package net.dodian.jobs.impl;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.npc.NpcUpdating;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.entity.player.PlayerUpdating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flushes outbound entity update packets after simulation/actions for the tick are complete.
 */
public class OutboundPacketProcessor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(OutboundPacketProcessor.class);
    private static final boolean DEBUG_ADDED_LOCAL_PLAYERS = false;
    private static final boolean DEBUG_NPC_MOVEMENT_WRITES = false;

    @Override
    public void run() {
        if (DEBUG_ADDED_LOCAL_PLAYERS) {
            PlayerUpdating.resetDebugAddedLocalCounter();
        }

        for (int i = 0; i < Constants.maxPlayers; i++) {
            Client player = (Client) PlayerHandler.players[i];
            if (player == null || !player.isActive) {
                continue;
            }
            updatePlayer(player, i);
        }

        for (Npc npc : Server.npcManager.getNpcs()) {
            if (npc != null) {
                npc.clearUpdateFlags();
            }
        }
        for (int i = 0; i < Constants.maxPlayers; i++) {
            Client player = (Client) PlayerHandler.players[i];
            if (player != null && player.isActive) {
                player.clearUpdateFlags();
            }
        }

        if (DEBUG_ADDED_LOCAL_PLAYERS) {
            logger.info("addedLocalPlayers={}", PlayerUpdating.consumeDebugAddedLocalCounter());
        }
        if (DEBUG_NPC_MOVEMENT_WRITES) {
            logger.info("npcMovementWrites={}", NpcUpdating.consumeDebugMovementWriteCounter());
        }
    }

    private void updatePlayer(Client player, int playerIndex) {
        if (player.timeOutCounter >= 84) {
            player.disconnected = true;
            player.println_debug("\nRemove non-responding " + player.getPlayerName() + " after 60 seconds of disconnect! ");
        }

        if (player.disconnected) {
            player.println_debug("\nRemove disconnected player " + player.getPlayerName());
            Server.playerHandler.removePlayer(player);
            player.disconnected = false;
            PlayerHandler.players[playerIndex] = null;
        } else {
            player.update();
        }
    }
}
