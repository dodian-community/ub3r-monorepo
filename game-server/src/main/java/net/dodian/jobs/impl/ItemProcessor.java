package net.dodian.jobs.impl;

import net.dodian.uber.game.model.item.Ground;
import net.dodian.uber.game.model.item.GroundItem;

public class ItemProcessor implements Runnable {
    @Override
    public void run() {
        if (!Ground.tradeable_items.isEmpty()) {
            for (GroundItem item : Ground.tradeable_items) {
                item.reduceTime();
                if(!item.isTaken() && !item.isVisible() && item.timeToShow < 1) {
                    item.visible = true;
                    item.itemDisplay();
                } else if (item.getDespawnTime() < 1)
                    Ground.deleteItem(item);
            }
        }
        /* Not tradeble items on ground */
        if (!Ground.untradeable_items.isEmpty()) {
            for (GroundItem item : Ground.untradeable_items) {
                item.reduceTime();
                if (item.getDespawnTime() < 1)
                    Ground.deleteItem(item);
            }
        }
        /* Global ground items */
        if (!Ground.ground_items.isEmpty()) {
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
