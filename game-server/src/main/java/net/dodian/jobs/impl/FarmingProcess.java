package net.dodian.jobs.impl;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class FarmingProcess implements Job {
    public void execute(JobExecutionContext context) throws JobExecutionException {
        for (Player p : PlayerHandler.players) {
            if(p != null) {
                Client c = ((Client) p);
                c.farming.updateFarming(c);
            }
        }
    }
}