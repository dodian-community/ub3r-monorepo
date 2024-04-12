package net.dodian.jobs.impl;

import net.dodian.uber.game.model.item.Ground;
import net.dodian.uber.game.model.item.GroundItem;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class ItemProcessor implements Job {
    public void execute(JobExecutionContext context) throws JobExecutionException {
        /* Not tradeble items on ground */
        if (!Ground.untradeable_items.isEmpty() || !(Ground.untradeable_items.size() < 0)) {
            for (GroundItem item : Ground.untradeable_items) {
                item.reduceTime();
                if (item.getDespawnTime() < 1)
                    Ground.deleteItem(item);
            }
        }
        /* Global ground items */
        if (!Ground.ground_items.isEmpty() || !(Ground.ground_items.size() < 0)) {
            for (GroundItem item : Ground.ground_items) {
                if(item.isVisible() || !item.isTaken()) continue;
                item.reduceTime();
                if (item.getDespawnTime() < 1) {
                    item.setTaken(false);
                    item.visible = true;
                    item.timeToDespawn = item.display;
                    Ground.addItem(item);
                }
            }
        }
    }

}