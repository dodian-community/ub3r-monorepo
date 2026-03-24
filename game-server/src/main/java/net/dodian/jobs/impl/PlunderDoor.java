package net.dodian.jobs.impl;

import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.skills.thieving.plunder.PyramidPlunderService;
import net.dodian.utilities.Misc;

import java.util.ArrayList;
import java.util.Arrays;

public class PlunderDoor implements Runnable {
    int hourTick = 4;
    @Override
    public void run() {
        var state = PyramidPlunderService.global();

        hourTick--;
        if(hourTick == 0) {
            /* Set entry door */
            ArrayList<Position> cloneDoors = new ArrayList<>(Arrays.asList(state.allDoors));
            cloneDoors.remove(state.currentDoor);
            if (!cloneDoors.isEmpty()) {
                int random = Misc.random(cloneDoors.size() - 1);
                state.currentDoor = cloneDoors.get(random);
            }
            hourTick = 4;
        }
        /* Set pyramid next door in all rooms except last! */
        for(int i = 0; i < state.nextRoom.length; i++)
            state.nextRoom[i] = Misc.random(3);
    }

}
