package net.dodian.jobs.impl;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.Position;
import net.dodian.utilities.Misc;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.ArrayList;
import java.util.Arrays;

public class PlunderDoor implements Job {
    int hourTick = 4;
    public void execute(JobExecutionContext context) throws JobExecutionException {
        hourTick--;
        if(hourTick == 0) {
            /* Set entry door */
            ArrayList<Position> cloneDoors = (ArrayList<Position>) Arrays.asList(Server.entryObject.allDoors);
            cloneDoors.remove(Server.entryObject.currentDoor);
            int random = Misc.random(cloneDoors.size() - 1);
            Server.entryObject.currentDoor = cloneDoors.get(random);
            hourTick = 4;
        }
        /* Set pyramid next door in all rooms except last! */
        for(int i = 0; i < Server.entryObject.nextRoom.length; i++)
            Server.entryObject.nextRoom[i] = Misc.random(3);
    }

}