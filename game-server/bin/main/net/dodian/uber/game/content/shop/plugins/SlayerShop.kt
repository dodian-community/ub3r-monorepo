package net.dodian.uber.game.content.shop.plugins

import net.dodian.uber.game.content.shop.plugin.ShopPlugin
import net.dodian.uber.game.content.shop.plugin.shopPlugin

object SlayerShop : ShopPlugin {
    override val definition =
        shopPlugin(name = "Slayer Shop", shopId = 15) {
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
