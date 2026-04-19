package net.dodian.uber.game.shop.catalog

import net.dodian.uber.game.shop.ShopPlugin
import net.dodian.uber.game.shop.ShopId
import net.dodian.uber.game.shop.shop

object CapeShop : ShopPlugin {
    override val definition =
        shop(shopId = ShopId.CAPE_SHOP) {
            sellModifier = 2
            buyModifier = 2
            requiresPremium = true
            item(9747, 1)
            item(9753, 1)
            item(9750, 1)
            item(9768, 1)
            item(9756, 1)
            item(9759, 1)
            item(9762, 1)
            item(9801, 1)
            item(9807, 1)
            item(9783, 1)
            item(9798, 1)
            item(9804, 1)
            item(9780, 1)
            item(9795, 1)
            item(9792, 1)
            item(9774, 1)
            item(9771, 1)
            item(9777, 1)
            item(9786, 1)
            item(9765, 1)
        }.definition
}

