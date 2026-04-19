package net.dodian.uber.game.shop.catalog

import net.dodian.uber.game.shop.ShopPlugin
import net.dodian.uber.game.shop.ShopId
import net.dodian.uber.game.shop.shop

object GeneralStoreShop : ShopPlugin {
    override val definition =
        shop(shopId = ShopId.GENERAL_STORE) {
            sellModifier = 1
            buyModifier = 1
            item(590, 2)
            item(2347, 5)
            item(946, 5)
            item(1351, 10)
            item(1265, 10)
            item(1755, 5)
            item(4155, 10)
            item(1735, 10)
            item(1925, 1337)
        }.definition
}

