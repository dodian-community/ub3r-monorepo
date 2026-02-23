package net.dodian.jobs.impl;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.object.GlobalObject;

public class ObjectProcess implements Runnable {
    @Override
    public void run() {
        for (int i = 0; i < Constants.maxPlayers; i++) {
            Client c = ((Client) (PlayerHandler.players[i]));
            if (c != null)
                GlobalObject.updateObject(c);
        }
    }

}
