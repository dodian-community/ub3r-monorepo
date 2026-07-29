package net.dodian.uber.game.economy

import net.dodian.uber.game.model.entity.player.Client

/**
 * Shop packet boundary.  The legacy implementation remains temporarily on
 * Client while its inventory mutations are migrated one operation at a time;
 * callers no longer reach Client directly.
 */
object ShopTransactionService {
    @JvmStatic fun sell(client: Client, itemId: Int, slot: Int, amount: Int) =
        client.performShopSell(itemId, slot, amount)

    @JvmStatic fun buy(client: Client, itemId: Int, slot: Int, amount: Int) =
        client.performShopBuy(itemId, slot, amount)
}
