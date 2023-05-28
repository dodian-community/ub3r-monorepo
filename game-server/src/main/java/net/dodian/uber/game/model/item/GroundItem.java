package net.dodian.uber.game.model.item;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.player.packets.outgoing.CreateGroundItem;

public class GroundItem {
    public int x, y, z, id, amount, dropper, playerId = -1, npcId = -1;
    public long dropped = 0;
    public boolean visible = false, npc = false;
    public boolean taken = false, canDespawn = true;
    public int timeDespawn = 120000, timeDisplay = 60000; //60k = 60 seconds!, 120000, 60000
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
        this.canDespawn = false;
        this.timeDisplay = display;
        dropped = System.currentTimeMillis();
    }

    public void setTaken(boolean b) {
        this.taken = b;
    }

    public boolean isTaken() {
        return taken;
    }

}
