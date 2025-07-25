package net.dodian.uber.game.model.player.skills.thieving;

import net.dodian.jobs.JobScheduler;
import net.dodian.jobs.impl.PlunderDoor;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.utilities.Misc;

public class PyramidPlunder {
    public Position currentDoor = null;
    public Position[] allDoors = {new Position(3288, 2799), new Position(3293, 2794), new Position(3288, 2789), new Position(3283, 2794)};
    public int[] nextRoom = {-1, -1, -1, -1, -1, -1, -1};

    Client c;
    private int ticks = -1, roomNr = 0;
    public Position start = new Position(1927, 4477, 0), end = new Position(3289, 2801, 0);
    private final Position[] room = {new Position(1954, 4477, 0),
        new Position(1977, 4471, 0), new Position(1927, 4453, 0), new Position(1965, 4444, 0),
        new Position(1927, 4424, 0), new Position(1943, 4421, 0), new Position(1974, 4420, 0)
    };
    public boolean looting = false;
    public int[] obstacles = {
            26616, 0, 26626, 0, 26618, 0, 26619, 0, 26620, 0, 26621, 0,
            26580, 0, 26600, 0, 26601, 0, 26602, 0, 26603, 0, 26604, 0,
            26605, 0, 26606, 0, 26607, 0, 26608, 0, 26609, 0, 26610, 0,
            26611, 0, 26612, 0, 26613, 0
    };
    public int[] urn_config = { 0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28 }; //Urns open, +1 to get snake!
    public int[] tomb_config = { 2, 0, 9, 10, 11, 12 }; //Golden chest + Sarcophagus + Tomb doors

    public PyramidPlunder() {
        /* Set entry door */
        Position[] doorCopy = allDoors.clone();
        int random = Misc.random(allDoors.length -1);
        currentDoor = doorCopy[random];
        /* Set pyramid next door in all rooms except last! */
        for(int i = 0; i < nextRoom.length; i++)
            nextRoom[i] = Misc.random(3);
        /* Timer every 15 minute to reset stuff! */
        JobScheduler.ScheduleRepeatForeverJob(900_000, PlunderDoor.class);
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
        ticks = 500;
        c.transport(start);
        c.send(new SendMessage("Starting plunder test..."));
    }
    public void resetPlunder() {
        if(ticks == 0) c.send(new SendMessage("You have run out time!"));
        ticks = -1;
        c.transport(end);
        roomNr = 0;
        resetObstacles();
    }
    public void toggleObstacles(int object) {
        if(looting) { //Prevent people from looting while they attempt to loot!
            return;
        }
        boolean found = false;
        for(int i = tomb_config.length; i < tomb_config.length + urn_config.length && !found; i++) { //Urn checks
            int checkSlot = obstacles.length / (tomb_config.length + urn_config.length);
            if(obstacles[i * checkSlot] == object && obstacles[i * checkSlot + 1] == 0) { //Reward check!
                //looting = true;
                obstacles[i * checkSlot + 1] = Misc.chance(2);
                displayUrns();
                found = true;
            }
        }
        if(!found && obstacles[0] == object) { //TODO: Fix formula for the items!
            found = true;
            if(obstacles[1] == 0) {
                obstacles[1] = 1;
                displayTomb();
                c.send(new SendMessage("Room: " + roomNr + " trying to do gold chest!"));
            }
        }
        if(!found && obstacles[2] == object) { //TODO: Fix formula for the items!
            found = true;
            if(obstacles[3] == 0) {
                obstacles[3] = 1;
                displayTomb();
                c.send(new SendMessage("Sarcophagus!"));
            }
        }
        for(int i = 2; i < tomb_config.length && !found; i++) {
            int checkSlot = obstacles.length / (tomb_config.length + urn_config.length);
            if(obstacles[i * checkSlot] == object && obstacles[i * checkSlot + 1] == 0) { //Pick-lock Tomb door
                found = true;
                obstacles[i * checkSlot + 1] = 1;
                displayTomb();
            }
        }
    }
    public boolean openDoor(int object) {
        for(int i = 2; i < tomb_config.length; i++) {
            int checkSlot = obstacles.length / (tomb_config.length + urn_config.length);
            if(obstacles[i * checkSlot] == object && obstacles[i * checkSlot + 1] == 1)
                return true;
        }
        return false;
    }
    public void resetObstacles() {
        c.varbit(820, 0);
        c.varbit(821, 0);
        for(int i = 0; i < tomb_config.length + urn_config.length; i++) { //Urn checks
            int checkSlot = obstacles.length / (tomb_config.length + urn_config.length);
            obstacles[i * checkSlot + 1] = 0;
        }
    }
    public void displayUrns() {
        int config = 0;
        for(int i = tomb_config.length; i < tomb_config.length + urn_config.length; i++) {
            int checkSlot = obstacles.length / (tomb_config.length + urn_config.length);
            int slot = i - tomb_config.length;
            if(obstacles[(i * checkSlot) + 1] == 1) config |= 1 << urn_config[slot];
            else if(obstacles[(i * checkSlot) + 1] == 2) config |= 1 << urn_config[slot] + 1; //Snake!
            //config |= 1 << (obstacles[i * checkSlot + 1] == 1 ? urn_config[slot] : 0);
        }
        c.varbit(820, config);
    }
    public void displayTomb() {
        int config = 0;
        for(int i = 0; i < tomb_config.length; i++) {
            int checkSlot = obstacles.length / (tomb_config.length + urn_config.length);
            if(obstacles[(i * checkSlot) + 1] == 1) config |= 1 << tomb_config[i];
        }
        c.varbit(821, config);
    }

    public boolean hinderTeleport() {
        return ticks != -1;
    }
    public int getRoomNr() {
        return roomNr;
    }
    public void nextRoom() {
        if(roomNr + 1 == 8) { //Maximum amount of rooms!
            return;
        }
        int level = 31 + (roomNr * 10);
        if(c.getLevel(Skill.THIEVING) < level) {
            c.send(new SendMessage("You need level " + level + " thieving to enter next room!"));
            return;
        }
        c.transport(room[roomNr]);
        resetObstacles();
        roomNr++;
    }

}
