package net.dodian.uber.game.shop.catalog

import net.dodian.uber.game.shop.ShopPlugin
import net.dodian.uber.game.shop.ShopId
import net.dodian.uber.game.shop.shop

object ChristmasEventStoreShop : ShopPlugin {
    override val definition =
        shop(shopId = ShopId.CHRISTMAS_EVENT_STORE) {
            sellModifier = 2
            buyModifier = 3
            currencyItemId = 11997
            buyPrice(slot = 0, price = 8000)
            buyPrice(slot = 1, price = 12000)
            buyPrice(slot = 2, price = 12000)
            buyPrice(slot = 3, price = 4000)
            buyPrice(slot = 4, price = 4000)
            buyPrice(slot = 5, price = 25000)
            buyPrice(slot = 6, price = 15000)
            item(12887, 100)
            item(12888, 100)
            item(12889, 100)
            item(12890, 100)
            item(12891, 100)
            item(10507, 100)
            item(12854, 1000)
        }.definition
}

