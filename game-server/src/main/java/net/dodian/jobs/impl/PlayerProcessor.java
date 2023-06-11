package net.dodian.jobs.impl;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class PlayerProcessor implements Job {
    public void execute(JobExecutionContext context) throws JobExecutionException {
        long currentTime = System.currentTimeMillis();
        try {
            Server.playerHandler.updatePlayerNames();
            PlayerHandler.cycle = PlayerHandler.cycle >= 100 ? 0 : PlayerHandler.cycle + 1; //Every minute reset cycle!
            if (PlayerHandler.cycle % 50 == 0) { //Every 50 tick aka 30 second we clear connections!
                Server.connections.clear();
                Server.nullConnections = 0;
            }
            for (int i = 0; i < Constants.maxPlayers; i++) {
                if (PlayerHandler.players[i] != null) {
                    /* Process should happend last! */
                    PlayerHandler.players[i].process();
                    while (PlayerHandler.players[i].packetProcess()); //Need a while loop atm for dodian's packet code!
                    PlayerHandler.players[i].postProcessing();
                    PlayerHandler.players[i].getNextPlayerMovement();
                    /* Player online checks */
                    long lp = currentTime - PlayerHandler.players[i].lastPacket;
                    if (PlayerHandler.players[i].dbId > 0 && lp >= 60000) { //Remove player after 60 seconds
                        PlayerHandler.players[i].disconnected = true;
                    }
                    if (PlayerHandler.players[i].disconnected) {
                        if (PlayerHandler.players[i].saveNeeded) {
                            ((Client) (PlayerHandler.players[i])).saveStats(true);
                        }
                        Server.playerHandler.removePlayer(PlayerHandler.players[i]);
                        PlayerHandler.players[i] = null;
                    } else {
                        if(!PlayerHandler.players[i].initialized) {
                            PlayerHandler.players[i].initialize();
                            PlayerHandler.players[i].initialized = true;
                        } else {
                            PlayerHandler.players[i].update();
                        }
                    }
                    /* end */
                    if (PlayerHandler.players[i] != null) { //Due to above code need this here!
                        PlayerHandler.players[i].clearUpdateFlags();
                        if (Server.updateRunning && PlayerHandler.players[i].updateAnnounced)
                            PlayerHandler.players[i].updateAnnounced = true;
                    }
                }
            }
            /* Server update! */
            if(Server.updateRunning) Server.updateElapsed += 0.6;
            if (Server.updateRunning
                    && currentTime - Server.updateStartTime > (Server.updateSeconds * 1000L)) {
                if (PlayerHandler.getPlayerCount() < 1) {
                    System.exit(0);
                }
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }

}