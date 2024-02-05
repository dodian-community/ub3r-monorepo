package net.dodian.jobs.impl;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.object.GlobalObject;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import static net.dodian.uber.game.Server.playerHandler;

@DisallowConcurrentExecution

public class ObjectProcess implements Job {
    public void execute(JobExecutionContext context) throws JobExecutionException {

    }

}