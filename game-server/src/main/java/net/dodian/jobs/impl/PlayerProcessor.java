package net.dodian.jobs.impl;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.utilities.Utils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class PlayerProcessor implements Job {
    public void execute(JobExecutionContext context) throws JobExecutionException {
        long currentTime = System.currentTimeMillis();
        Server.playerHandler.updatePlayerNames();
        if (PlayerHandler.cycle % 10 == 0) {
            Server.connections.clear();
            Server.nullConnections = 0;
        }
        if (PlayerHandler.cycle % 100 == 0) {
            Server.banned.clear();
        }
        PlayerHandler.cycle = PlayerHandler.cycle >= 1000 ? 0 : PlayerHandler.cycle + 1;
        for (int i = 0; i < Constants.maxPlayers; i++) {
            if (PlayerHandler.players[i] == null || PlayerHandler.players[i].disconnected)
                continue;
            try {
                if (!PlayerHandler.players[i].isActive) {
                    if (PlayerHandler.players[i].violations > 100) {
                        System.out.println("Disconnecting bugged player " + PlayerHandler.players[i].getPlayerName());
                        Server.playerHandler.removePlayer(PlayerHandler.players[i]);
                        PlayerHandler.players[i] = null;
                    } else {
                        PlayerHandler.players[i].violations++;
                    }
                    continue;
                }
                if (PlayerHandler.players[i].dbId < 1
                        && System.currentTimeMillis() - PlayerHandler.players[i].lastPacket >= 60000) {
                    Utils.println("Disconnecting possible null " + PlayerHandler.players[i].getPlayerName());
                    PlayerHandler.players[i].disconnected = true;
                }
                PlayerHandler.players[i].actionAmount--;
                try {
                    PlayerHandler.players[i].preProcessing();
                    PlayerHandler.players[i].packetProcess();
                    PlayerHandler.players[i].process();
                    PlayerHandler.players[i].postProcessing();
                    PlayerHandler.players[i].getNextPlayerMovement();
                } catch(Exception e) {
                    PlayerHandler.players[i].disconnected = true;
                }
                    /*PlayerHandler.players[i].preProcessing(); //This is whatever!
                    PlayerHandler.players[i].process();
                    while (PlayerHandler.players[i].packetProcess());
                    PlayerHandler.players[i].postProcessing();
                     */
                if (PlayerHandler.players[i].getPlayerName().equalsIgnoreCase(PlayerHandler.kickNick)) {
                    PlayerHandler.players[i].kick();
                    PlayerHandler.kickNick = "";
                }
            } catch (Exception e) {
                if (!PlayerHandler.players[i].getPlayerName().equals("null"))
                    System.out.println("Error with player " + i + ", " + PlayerHandler.players[i].getPlayerName());
                e.printStackTrace();
            }
        }

        // loop through all players and do the updating stuff
        for (int i = 0; i < Constants.maxPlayers; i++) {
            if (PlayerHandler.players[i] == null || !PlayerHandler.players[i].isActive|| PlayerHandler.players[i].getPlayerName() == null
                    || PlayerHandler.players[i].getPlayerName().equals("null"))
                continue;
            long lp = currentTime - PlayerHandler.players[i].lastPacket;
            if (PlayerHandler.players[i].dbId > 0 && lp >= 60000) {
                System.out.println("Removing non-responding player " + PlayerHandler.players[i].getPlayerName());
                PlayerHandler.players[i].disconnected = true;
            }
            if (PlayerHandler.players[i].disconnected) {
                if (PlayerHandler.players[i].saveNeeded) {
                    ((Client) (PlayerHandler.players[i])).saveStats(true);
                }
                Server.playerHandler.removePlayer(PlayerHandler.players[i]);
                PlayerHandler.players[i] = null;
            } else {
                if (!PlayerHandler.players[i].initialized) {
                    PlayerHandler.players[i].initialize();
                    PlayerHandler.players[i].initialized = true;
                } else {
                    try {
                        PlayerHandler.players[i].update();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        // post processing
        for (int i = 0; i < Constants.maxPlayers; i++) {
            if (PlayerHandler.players[i] == null || !PlayerHandler.players[i].isActive)
                continue;
            try {
                PlayerHandler.players[i].clearUpdateFlags();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (Server.updateRunning && !Server.updateAnnounced) {
            Server.updateAnnounced = true;
        }
        if (Server.updateRunning
                && currentTime - Server.updateStartTime > (Server.updateSeconds * 1000L)) {
            if (PlayerHandler.getPlayerCount() < 1) {
                System.exit(0);
            }
        }

    }

}