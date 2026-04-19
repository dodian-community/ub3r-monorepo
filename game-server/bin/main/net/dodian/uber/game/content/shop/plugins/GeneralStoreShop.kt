package net.dodian.uber.game.content.shop.plugins

import net.dodian.uber.game.content.shop.plugin.ShopPlugin
import net.dodian.uber.game.content.shop.plugin.shopPlugin

object GeneralStoreShop : ShopPlugin {
    override val definition =
        shopPlugin(name = "General Store", shopId = 3) {
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
