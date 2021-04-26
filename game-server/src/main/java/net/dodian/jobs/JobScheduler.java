package net.dodian.jobs;

import net.dodian.uber.game.model.entity.Entity;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Created by fabrice on 6-6-2015.
 */
public class JobScheduler {

    private static Scheduler scheduler;
    private Entity entity;

    public JobScheduler() throws SchedulerException {
        scheduler = new StdSchedulerFactory().getScheduler();
        scheduler.start();
    }

    public JobScheduler(Entity entity) {
        this.entity = entity;
    }

    public void scheduleJob(int milliSeconds, Class<? extends Job> object) throws SchedulerException {
        JobDetail job = JobBuilder.newJob(object).build();

        job.getJobDataMap().put("entity", entity);

        Trigger trigger = TriggerBuilder.newTrigger().withIdentity(entity.toString(), object.getName())
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(milliSeconds).repeatForever())
                .build();

        scheduler.scheduleJob(job, trigger);
    }

    public boolean jobExists(Class<? extends Job> object) throws SchedulerException {
        if (getScheduler().checkExists(new TriggerKey(entity.toString(), object.getName()))) {
            return true;
        }
        return false;
    }

    public void deleteJob(Class<? extends Job> object) throws SchedulerException {
        if (getScheduler().checkExists(new TriggerKey(entity.toString(), object.getName()))) {
            getScheduler().unscheduleJob(new TriggerKey(entity.toString(), object.getName()));
        }
    }

    public static void ScheduleStaticRepeatForeverJob(int milliSeconds, Class<? extends Job> object)
            throws SchedulerException {
        JobDetail job = JobBuilder.newJob(object).build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(milliSeconds).repeatForever())
                .build();

        scheduler.scheduleJob(job, trigger);
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

}
