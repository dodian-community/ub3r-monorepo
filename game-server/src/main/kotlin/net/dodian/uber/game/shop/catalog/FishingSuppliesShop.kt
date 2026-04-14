package net.dodian.uber.game.shop.catalog

import net.dodian.uber.game.shop.ShopPlugin
import net.dodian.uber.game.shop.ShopId
import net.dodian.uber.game.shop.shop

object FishingSuppliesShop : ShopPlugin {
    override val definition =
        shop(shopId = ShopId.FISHING_SUPPLIES) {
            sellModifier = 2
            buyModifier = 2
            item(303, 1000)
            item(301, 1000)
            item(311, 1000)
            item(309, 1000)
            item(317, 0)
        }.definition
}

