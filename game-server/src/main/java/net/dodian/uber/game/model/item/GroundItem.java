package net.dodian.uber.game.model.item;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.netty.listener.out.CreateGroundItem;
import net.dodian.uber.game.netty.listener.out.RemoveGroundItem;

public class GroundItem {
    public int x, y, z, id, amount, dropper, playerId = -1, npcId = -1, type = 2;
    public boolean taken = false, visible = false, npc = false;
    public int timeToShow = 100, timeToDespawn = 200, display = 0; //100, 200, 0 default!

    public GroundItem(Position pos, int id, int amount, int dropper, int npcId) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.id = id;
        this.amount = amount;
        this.dropper = dropper;
        this.npc = npcId >= 0;
        if (npc)
            this.npcId = npcId;
        if(!Server.itemManager.isTradable(this.id)) { //untradeable items!!
            this.timeToDespawn = this.timeToDespawn + this.timeToShow;
            this.timeToShow = -1;
            this.type = 1;
        }
        if (dropper > 0 && Server.playerHandler.validClient(dropper)) {
            Server.playerHandler.getClient(dropper).send(new CreateGroundItem(new GameItem(id, amount), new Position(x, y, z)));
            playerId = Server.playerHandler.getClient(dropper).dbId;
        }
    }

    public GroundItem(Position pos, int id, int amount, int display, boolean visible) { //Mostly for Static ground items!
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.id = id;
        this.amount = amount;
        this.timeToShow = -1;
        this.timeToDespawn = display;
        this.display = display;
        this.type = 0;
        this.visible = visible;
    }

    public GroundItem(Position pos, int... drop) { //int dropper, int id, int amount, int display
        if(drop.length < 4) { return; } //Cant have this if length is less than 4!
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.id = drop[1];
        this.amount = drop[2];
        if(!Server.itemManager.isTradable(this.id) && drop.length == 4) { //untradeable items!!
            this.timeToDespawn = drop[3];
            this.timeToShow = -1;
            this.type = 1;
        }
        if (drop[0] >= 0 && Server.playerHandler.validClient(drop[0])) {
            Server.playerHandler.getClient(drop[0]).send(new CreateGroundItem(new GameItem(id, amount), new Position(x, y, z)));
            playerId = Server.playerHandler.getClient(drop[0]).dbId;
        }
    }

    public void setTaken(boolean b) {
        this.taken = b;
    }
    public boolean isTaken() {
        return taken;
    }
    public boolean isVisible() {
        return visible;
    }

    public void reduceTime() {
        if(type == 0 && isVisible() && getDespawnTime() > 0)
            timeToDespawn--;
        else if(timeToShow < 1 && getDespawnTime() > 0) {
            timeToDespawn--;
        } else if(getDespawnTime() > 0)
            timeToShow--;
    }
    public int getDespawnTime() {
        return timeToShow < 1 ? timeToDespawn : timeToShow + timeToDespawn;
    }

    public void removeItemDisplay() {
        for (int i = 0; i < Constants.maxPlayers; i++) {
            if (PlayerHandler.players[i] == null) continue;
            Client c = (Client) PlayerHandler.players[i];
            if(c.GoodDistance(c.getPosition().getX(), c.getPosition().getY(), this.x, this.y, 104))
                c.send(new RemoveGroundItem(new GameItem(id, amount), new Position(this.x, this.y, this.z)));
        }
    }
    public void itemDisplay() {
        for (int i = 0; i < Constants.maxPlayers; i++) {
            if (PlayerHandler.players[i] == null || (PlayerHandler.players[i] != null && type == 1 && playerId != PlayerHandler.players[i].dbId)) continue;
            Client c = (Client) PlayerHandler.players[i];
            if(c.GoodDistance(c.getPosition().getX(), c.getPosition().getY(), this.x, this.y, 104) && c.dbId != playerId && isVisible())
                c.send(new CreateGroundItem(new GameItem(id, amount), new Position(this.x, this.y, this.z)));
        }
    }

}
