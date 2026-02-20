package net.dodian.jobs.impl;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.Position;
import net.dodian.utilities.Misc;

import java.util.ArrayList;
import java.util.Arrays;

public class PlunderDoor implements Runnable {
    int hourTick = 4;
    @Override
    public void run() {
        if (Server.entryObject == null) {
            return;
        }

        hourTick--;
        if(hourTick == 0) {
            /* Set entry door */
            ArrayList<Position> cloneDoors = new ArrayList<>(Arrays.asList(Server.entryObject.allDoors));
            cloneDoors.remove(Server.entryObject.currentDoor);
            if (!cloneDoors.isEmpty()) {
                int random = Misc.random(cloneDoors.size() - 1);
                Server.entryObject.currentDoor = cloneDoors.get(random);
            }
            hourTick = 4;
        }
        /* Set pyramid next door in all rooms except last! */
        for(int i = 0; i < Server.entryObject.nextRoom.length; i++)
            Server.entryObject.nextRoom[i] = Misc.random(3);
    }

}
