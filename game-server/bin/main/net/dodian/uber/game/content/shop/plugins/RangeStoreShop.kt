package net.dodian.uber.game.content.shop.plugins

import net.dodian.uber.game.content.shop.plugin.ShopPlugin
import net.dodian.uber.game.content.shop.plugin.shopPlugin

object RangeStoreShop : ShopPlugin {
    override val definition =
        shopPlugin(name = "Range store", shopId = 11) {
            sellModifier = 2
            buyModifier = 2
            buyPriceMultiplier = 1.5
            item(839, 10)
            item(841, 10)
            item(843, 10)
            item(845, 10)
            item(847, 10)
            item(849, 10)
            item(1063, 10)
            item(1065, 10)
            item(1095, 10)
            item(1099, 10)
            item(1129, 10)
            item(1135, 10)
            item(882, 10000)
            item(884, 5000)
            item(886, 2000)
        }.definition
}
