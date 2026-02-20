package net.dodian.jobs.impl;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.model.entity.player.PlayerHandler;

public class FarmingProcess implements Runnable {
    @Override
    public void run() {
        for (Player p : PlayerHandler.players) {
            if(p != null) {
                Client c = ((Client) p);
                c.farming.updateFarming(c);
            }
        }
    }
}
