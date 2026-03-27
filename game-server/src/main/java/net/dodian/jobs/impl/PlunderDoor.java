package net.dodian.jobs.impl;

import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.content.skills.thieving.plunder.PyramidPlunderService;
import net.dodian.utilities.Misc;

import java.util.ArrayList;
import java.util.Arrays;

public class PlunderDoor implements Runnable {
    int hourTick = 4;
    @Override
    public void run() {
        hourTick--;
        if(hourTick == 0) {
            var state = PyramidPlunderService.global();
            ArrayList<Position> cloneDoors = new ArrayList<>(Arrays.asList(state.getAllDoors()));
            cloneDoors.remove(state.getCurrentDoor());
            if (!cloneDoors.isEmpty()) {
                int random = Misc.random(cloneDoors.size() - 1);
                state.setCurrentDoor(cloneDoors.get(random));
            }
            hourTick = 4;
        }
        PyramidPlunderService.resetGlobalCycleState();
    }

}
