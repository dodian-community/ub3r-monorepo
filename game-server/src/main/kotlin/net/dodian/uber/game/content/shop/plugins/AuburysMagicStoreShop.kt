package net.dodian.uber.game.content.shop.plugins

import net.dodian.uber.game.content.shop.plugin.ShopPlugin
import net.dodian.uber.game.content.shop.plugin.shopPlugin

object AuburysMagicStoreShop : ShopPlugin {
    override val definition =
        shopPlugin(name = "Aubury's Magic Store", shopId = 9) {
            sellModifier = 2
            buyModifier = 2
            buyPriceMultiplier = 1.5
            item(2415, 10)
            item(2416, 10)
            item(2417, 10)
            item(4089, 10)
            item(4091, 10)
            item(4093, 10)
            item(4095, 10)
            item(4097, 10)
        }.definition
}
