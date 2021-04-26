package net.dodian.jobs.impl;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.item.Ground;
import net.dodian.uber.game.model.item.GroundItem;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@DisallowConcurrentExecution
public class GroundItemProcessor implements Job {
    public void execute(JobExecutionContext context) throws JobExecutionException {

        if (Ground.items.size() < 0) {
            return;
        }
        long now = System.currentTimeMillis();
        for (GroundItem item : Ground.items) {
            if (!item.canDespawn && item.taken && now - item.dropped >= item.timeDisplay) {
                item.taken = false;
                item.visible = false;
            }
            if ((!item.visible || (!item.visible && !item.canDespawn && !item.taken))
                    && (now - item.dropped >= item.timeDisplay || !item.canDespawn)) {
                for (int i = 0; i < PlayerHandler.players.length; i++) {
                    Client p = Server.playerHandler.getClient(i);
                    if (p != null && Server.itemManager.isTradable(item.id) && p.dbId != item.playerId
                            && Math.abs(p.getPosition().getX() - item.x) < 114 && Math.abs(p.getPosition().getY() - item.y) < 114) {
                        p.createGroundItem(item.id, item.x, item.y, item.amount);
                    }
                }
                item.visible = true;
            }
            if (item.canDespawn && item.visible && now - item.dropped >= (item.timeDisplay + item.timeDespawn)) {
                Ground.deleteItem(item);
            }
        }
    }

}