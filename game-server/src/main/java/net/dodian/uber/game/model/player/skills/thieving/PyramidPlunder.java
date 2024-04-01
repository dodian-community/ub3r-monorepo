package net.dodian.uber.game.model.player.skills.thieving;

import net.dodian.jobs.JobScheduler;
import net.dodian.jobs.impl.PlunderDoor;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.utilities.Misc;

public class PyramidPlunder {
    Client c;
    public Position currentDoor = null;
    public Position[] allDoors = {new Position(3288, 2799), new Position(3293, 2794), new Position(3288, 2789), new Position(3283, 2794)};
    private int startTicks = 500, ticks = -1, roomNr = 0;
    public Position start = new Position(1927, 4477, 0), end = new Position(3289, 2801, 0);
    private Position[] room = {new Position(1954, 4477, 0),
        new Position(1977, 4471, 0), new Position(1927, 4453, 0), new Position(1965, 4444, 0),
        new Position(1927, 4424, 0), new Position(1943, 4421, 0), new Position(1974, 4420, 0)
    };
    public int[] nextRoom = {-1, -1, -1, -1, -1, -1, -1};

    public PyramidPlunder() {
        /* Set entry door */
        Position[] doorCopy = allDoors.clone();
        int random = Misc.random(allDoors.length -1);
        currentDoor = doorCopy[random];
        /* Set pyramid next door in all rooms except last! */
        for(int i = 0; i < nextRoom.length; i++)
            nextRoom[i] = Misc.random(3);
        /* Timer every 15 minute to reset stuff! */
        JobScheduler.ScheduleStaticRepeatForeverJob(900_000, PlunderDoor.class);
    }

    public PyramidPlunder(Client c) {
        this.c = c;
    }

    public boolean getEntryDoor(Position pos) {
        return currentDoor != null && currentDoor.equals(pos);
    }

    public void reduceTicks() {
        if(ticks == -1) { //Do nothing if ticks == -1!
            return;
        }
        ticks--;
        if(ticks == 0)
            resetPlunder();
        else if (ticks%100 == 0) c.send(new SendMessage("You got " + (ticks / 100) + " minute"+((ticks / 100) == 1 ? "" : "s")+" left."));
    }

    public void startPlunder() {
        ticks = startTicks;
        c.transport(start);
        c.send(new SendMessage("Starting plunder test..."));
    }
    public void resetPlunder() {
        if(ticks == 0) c.send(new SendMessage("You have run out time!"));
        ticks = -1;
        c.transport(end);
        roomNr = 0;
        /* Reset Gold chests*/
        /* Reset urns */
    }

    public boolean hinderTeleport() {
        return ticks != -1;
    }
    public int getRoomNr() {
        return roomNr;
    }
    public void nextRoom() { //Cant go back so.. hehe :D
        if(roomNr + 1 == 8) { //Maximum amount of rooms!
            return;
        }
        int level = 31 + (roomNr * 10);
        if(c.getLevel(Skill.THIEVING) < level) {
            c.send(new SendMessage("You need level " + level + " thieving to enter next room!"));
            return;
        }
        c.transport(room[roomNr]);
        roomNr++;
    }

}
