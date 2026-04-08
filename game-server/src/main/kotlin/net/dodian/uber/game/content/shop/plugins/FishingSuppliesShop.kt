package net.dodian.uber.game.content.shop.plugins

import net.dodian.uber.game.content.shop.plugin.ShopPlugin
import net.dodian.uber.game.content.shop.plugin.shopPlugin

object FishingSuppliesShop : ShopPlugin {
    override val definition =
        shopPlugin(name = "Fishing supplies", shopId = 40) {
            sellModifier = 2
            buyModifier = 2
            item(303, 1000)
            item(301, 1000)
            item(311, 1000)
            item(309, 1000)
            item(317, 0)
        }.definition
}
