package net.dodian.uber.game.content.shop.plugins

import net.dodian.uber.game.content.shop.plugin.ShopPlugin
import net.dodian.uber.game.content.shop.plugin.shopPlugin

object CraftingStoreShop : ShopPlugin {
    override val definition =
        shopPlugin(name = "Crafting store", shopId = 25) {
            sellModifier = 2
            buyModifier = 2
            item(1733, 10000)
            item(1734, 25000)
            item(1592, 10000)
            item(1597, 10000)
            item(1595, 10000)
            item(11065, 10000)
        }.definition
}
