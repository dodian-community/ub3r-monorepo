package net.dodian.jobs.impl;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.ShopHandler;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@DisallowConcurrentExecution

public class ShopProcessor implements Job {
    public void execute(JobExecutionContext context) throws JobExecutionException {
        boolean DidUpdate = false;
        for (int i = 1; i <= ShopHandler.MaxShops; i++) {
            if (ShopHandler.ShopItemsDelay[i] >= ShopHandler.MaxShowDelay) {
                for (int j = 0; j < ShopHandler.MaxShopItems; j++) {
                    if (ShopHandler.ShopItems[i][j] > 0) {
                        if (j < ShopHandler.ShopItemsStandard[i]
                                && ShopHandler.ShopItemsN[i][j] != ShopHandler.ShopItemsSN[i][j]) {
                            if (ShopHandler.ShopItemsN[i][j] < ShopHandler.ShopItemsSN[i][j]) {
                                double restockAmount = (ShopHandler.ShopItemsSN[i][j] - ShopHandler.ShopItemsN[i][j]) * 0.05;
                                ShopHandler.ShopItemsN[i][j] += restockAmount > 1 ? (int) restockAmount : 1;
                            } else Server.shopHandler.DiscountItem(i, j);
                        }
                        if (j >= ShopHandler.ShopItemsStandard[i]) Server.shopHandler.DiscountItem(i, j);
                        DidUpdate = true;
                    }
                }
            } else ShopHandler.ShopItemsDelay[i]++;
            if (DidUpdate) {
                for (int k = 1; k < Constants.maxPlayers; k++) {
                    if (PlayerHandler.players[k] != null) {
                        if (PlayerHandler.players[k].isShopping() && PlayerHandler.players[k].MyShopID == i) {
                            ((Client) PlayerHandler.players[k]).checkItemUpdate();
                        }
                    }
                }
                ShopHandler.ShopItemsDelay[i] = 0;
                DidUpdate = false;
            }
        }

    }

}