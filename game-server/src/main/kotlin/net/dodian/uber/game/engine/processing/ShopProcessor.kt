package net.dodian.uber.game.engine.processing

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.ShopHandler
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.world.player.PlayerRegistry

class ShopProcessor : Runnable {
    override fun run() {
        var didUpdate = false
        for (i in 1..ShopHandler.MaxShops) {
            if (ShopHandler.ShopItemsDelay.size > i) {
                if (ShopHandler.ShopItemsDelay[i] >= ShopHandler.MaxShowDelay) {
                    for (j in 0 until ShopHandler.MaxShopItems) {
                        if (ShopHandler.ShopItems[i][j] > 0) {
                            if (j < ShopHandler.ShopItemsStandard[i] && ShopHandler.ShopItemsN[i][j] != ShopHandler.ShopItemsSN[i][j]) {
                                if (ShopHandler.ShopItemsN[i][j] < ShopHandler.ShopItemsSN[i][j]) {
                                    val restockAmount = (ShopHandler.ShopItemsSN[i][j] - ShopHandler.ShopItemsN[i][j]) * 0.05
                                    ShopHandler.ShopItemsN[i][j] += if (restockAmount > 1) restockAmount.toInt() else 1
                                } else {
                                    Server.shopHandler.DiscountItem(i, j)
                                }
                            }
                            if (j >= ShopHandler.ShopItemsStandard[i]) {
                                Server.shopHandler.DiscountItem(i, j)
                            }
                            didUpdate = true
                        }
                    }
                } else {
                    ShopHandler.ShopItemsDelay[i]++
                }
            }

            if (didUpdate) {
                PlayerRegistry.forEachActivePlayer { player ->
                    if (player.isShopping && player.MyShopID == i) {
                        (player as Client).checkItemUpdate()
                    }
                }
                ShopHandler.ShopItemsDelay[i] = 0
                didUpdate = false
            }
        }
    }
}
