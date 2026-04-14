package net.dodian.uber.game.shop.catalog

import net.dodian.uber.game.shop.ShopPlugin
import net.dodian.uber.game.shop.ShopId
import net.dodian.uber.game.shop.shop

object SlayerShop : ShopPlugin {
    override val definition =
        shop(shopId = ShopId.SLAYER_SHOP) {
            sellModifier = 2
            buyModifier = 2
            item(4170, 1)
            item(4156, 1)
            item(4158, 1)
            item(4160, 1)
            item(5553, 1)
            item(5554, 1)
            item(5555, 1)
            item(5556, 1)
            item(5557, 1)
            item(6585, 100)
        }.definition
}

