package net.dodian.jobs;

import net.dodian.uber.game.model.entity.Entity;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Created by fabrice on 6-6-2015.
 */
public class JobScheduler {

    private static Scheduler scheduler;
    private final Entity entity;

    public JobScheduler(Entity entity) {
        this.entity = entity;
    }

    public void scheduleJob(int milliSeconds, Class<? extends Job> object, Entity entity) throws SchedulerException {
        JobDetail job = JobBuilder.newJob(object).build();

        job.getJobDataMap().put("entity", entity);

        Trigger trigger = TriggerBuilder.newTrigger().withIdentity(entity.toString(), object.getName())
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(milliSeconds).repeatForever())
                .build();

        startJobScheduler();
        scheduler.scheduleJob(job, trigger);
    }

    public boolean jobExists(Class<? extends Job> object) throws SchedulerException {
        return getScheduler().checkExists(new TriggerKey(entity.toString(), object.getName()));
    }

    public void deleteJob(Class<? extends Job> object) throws SchedulerException {
        if (getScheduler().checkExists(new TriggerKey(entity.toString(), object.getName()))) {
            getScheduler().unscheduleJob(new TriggerKey(entity.toString(), object.getName()));
        }
    }

    public static void ScheduleRepeatForeverJob(int milliSeconds, Class<? extends Job> object) {
        JobDetail job = JobBuilder.newJob(object).build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(milliSeconds).repeatForever())
                .build();
        try {
            startJobScheduler();
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            //TODO: Add exceptions stored debug!
        }
    }

    public static void startJobScheduler() throws SchedulerException {
        scheduler = new StdSchedulerFactory().getScheduler();
        scheduler.start();
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

}
