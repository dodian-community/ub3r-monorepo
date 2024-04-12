package net.dodian.jobs.impl;

import net.dodian.uber.game.model.item.Ground;
import net.dodian.uber.game.model.item.GroundItem;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class GroundItemProcessor implements Job {
    public void execute(JobExecutionContext context) throws JobExecutionException {
        if (!Ground.tradeable_items.isEmpty() || !(Ground.tradeable_items.size() < 0)) {
            for (GroundItem item : Ground.tradeable_items) {
                item.reduceTime();
                if(!item.isTaken() && !item.isVisible() && item.timeToShow < 1) {
                    item.visible = true;
                    item.itemDisplay();
                } else if (item.getDespawnTime() < 1)
                    Ground.deleteItem(item);
            }
        }
    }
}