package net.dodian.uber.game.content.shop.plugins

import net.dodian.uber.game.content.shop.plugin.ShopPlugin
import net.dodian.uber.game.content.shop.plugin.shopPlugin

object JatixHerbloreStoreShop : ShopPlugin {
    override val definition =
        shopPlugin(name = "Jatix herblore Store", shopId = 39) {
            sellModifier = 2
            buyModifier = 2
            item(233, 100)
            item(229, 10000)
            item(11877, 100)
            item(227, 10000)
            item(11879, 100)
        }.definition
}
