package net.dodian.jobs.impl;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.utilities.Utils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import static net.dodian.DotEnvKt.getGameWorldId;

public class WorldProcessor implements Job {
    private long lastPlayerUpdate;

    public void execute(JobExecutionContext context) throws JobExecutionException {
        long currentTime = System.currentTimeMillis();
        try {
            if (currentTime - lastPlayerUpdate >= 60000) {
                lastPlayerUpdate = currentTime;
                if (getGameWorldId() == 1) {
                    Server.login.sendPlayers();
                }
                int latestNews = Server.login.latestNews();
                for (int i = 0; i < PlayerHandler.players.length; i++) {
                    Client c = ((Client) PlayerHandler.players[i]);
                    Player p = PlayerHandler.players[i];
                    if(p != null && c.loadingDone) { //If active player.
                        if(p.latestNews != latestNews) {
                            p.latestNews = latestNews;
                            c.send(new SendMessage("[SERVER]: There is a new post on the homepage! type ::news"));
                        }
                    }
                }
                Server.chat.clear(); //Not sure what this do, but empty it just incase!
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("world process ended for some reason...");
            System.out.println(e.getMessage());
        }

    }

}