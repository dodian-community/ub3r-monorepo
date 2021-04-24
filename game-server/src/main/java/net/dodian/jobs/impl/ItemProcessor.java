package net.dodian.jobs.impl;

import net.dodian.uber.game.model.item.ItemHandler;
import net.dodian.utilities.Utils;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@DisallowConcurrentExecution

public class ItemProcessor implements Job {
  public void execute(JobExecutionContext context) throws JobExecutionException {

    for (int i = 0; i <= 8000; i++) {
      if (ItemHandler.globalItemID[i] != 0)
        ItemHandler.globalItemTicks[i]++;

      if ((ItemHandler.hideItemTimer + ItemHandler.showItemTimer) == ItemHandler.globalItemTicks[i]) {
        if (!ItemHandler.globalItemStatic[i]) {
          ItemHandler.removeItemAll(ItemHandler.globalItemID[i], ItemHandler.globalItemX[i],
              ItemHandler.globalItemY[i]);
        } else {
          Utils.println("Item is static");
        }
      }

      if (ItemHandler.showItemTimer == ItemHandler.globalItemTicks[i]) { // Phate:
                                                                         // Item
                                                                         // has
                                                                         // expired,
                                                                         // show
                                                                         // to
                                                                         // all
        if (!ItemHandler.globalItemStatic[i]) {
          ItemHandler.createItemAll(ItemHandler.globalItemID[i], ItemHandler.globalItemX[i], ItemHandler.globalItemY[i],
              ItemHandler.globalItemAmount[i], ItemHandler.globalItemController[i]);
        } else
          Utils.println("Item is static");
      }

    }

  }

}