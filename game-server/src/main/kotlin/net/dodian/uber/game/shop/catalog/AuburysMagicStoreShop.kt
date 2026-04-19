package net.dodian.uber.game.shop.catalog

import net.dodian.uber.game.shop.ShopPlugin
import net.dodian.uber.game.shop.ShopId
import net.dodian.uber.game.shop.shop

object AuburysMagicStoreShop : ShopPlugin {
    override val definition =
        shop(shopId = ShopId.AUBURYS_MAGIC_STORE) {
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

