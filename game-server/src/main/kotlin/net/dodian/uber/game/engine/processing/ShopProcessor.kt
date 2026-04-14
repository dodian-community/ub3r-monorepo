package net.dodian.uber.game.engine.processing

import net.dodian.uber.game.Server
import net.dodian.uber.game.shop.ShopManager
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry

class ShopProcessor : Runnable {
    override fun run() {
        var didUpdate = false
        for (i in 0 until ShopManager.MaxShops) {
            if (ShopManager.ShopItemsDelay[i] >= ShopManager.MaxShowDelay) {
                for (j in 0 until ShopManager.MaxShopItems) {
                    if (ShopManager.ShopItems[i][j] > 0) {
                        if (j < ShopManager.ShopItemsStandard[i] && ShopManager.ShopItemsN[i][j] != ShopManager.ShopItemsSN[i][j]) {
                            if (ShopManager.ShopItemsN[i][j] < ShopManager.ShopItemsSN[i][j]) {
                                val restockAmount = (ShopManager.ShopItemsSN[i][j] - ShopManager.ShopItemsN[i][j]) * 0.05
                                ShopManager.ShopItemsN[i][j] += if (restockAmount > 1) restockAmount.toInt() else 1
                            } else {
                                Server.shopManager.DiscountItem(i, j)
                            }
                        }
                        if (j >= ShopManager.ShopItemsStandard[i]) {
                            Server.shopManager.DiscountItem(i, j)
                        }
                        didUpdate = true
                    }
                }
            } else {
                ShopManager.ShopItemsDelay[i]++
            }

            if (didUpdate) {
                PlayerRegistry.forEachActivePlayer { player ->
                    if (player.isShopping && player.MyShopID == i) {
                        (player as Client).checkItemUpdate()
                    }
                }
                ShopManager.ShopItemsDelay[i] = 0
                didUpdate = false
            }
        }
    }
}
