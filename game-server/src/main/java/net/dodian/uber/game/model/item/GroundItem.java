package net.dodian.uber.game.model.item;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.player.packets.outgoing.CreateGroundItem;

public class GroundItem {
    public int x, y, z, id, amount, dropper, playerId = -1, npcId = -1;
    public long dropped = 0;
    public boolean visible = false, npc = false;
    public boolean taken = false, canDespawn = true;
    public int timeDisplay = 60_000, timeDespawn = timeDisplay + 90_000; //timeDisplay = your display time, timeDespawn = time after dropped to despawn! current 30 sec!
    public GroundItem(Position pos, int id, int amount, int dropper, int npcId) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.id = id;
        this.amount = amount;
        this.dropper = dropper;
        this.npc = npcId >= 0 ? true : false;
        if (npc)
            this.npcId = npcId;
        this.canDespawn = true;
        dropped = System.currentTimeMillis();
        if (dropper > 0 && Server.playerHandler.validClient(dropper)) {
            Server.playerHandler.getClient(dropper).send(new CreateGroundItem(new GameItem(id, amount), new Position(x, y, z)));
            playerId = Server.playerHandler.getClient(dropper).dbId;
        }
    }

    public GroundItem(Position pos, int id, int amount, int display) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.id = id;
        this.amount = amount;
        dropped = System.currentTimeMillis();
        this.canDespawn = false;
        this.timeDisplay = display;
    }

    public GroundItem(Position pos, int... drop) { //int dropper, int id, int amount, int display
        if(drop.length < 4) { return; } //Cant have this if length is less than 4!
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.id = drop[1];
        this.amount = drop[2];
        this.timeDisplay = 0;
        this.timeDespawn = drop[3] * 1000;
        this.canDespawn = true;
        dropped = System.currentTimeMillis();
        if (drop[0] > 0 && Server.playerHandler.validClient(drop[0])) {
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

}
