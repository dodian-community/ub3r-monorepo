package net.dodian.uber.game.shop.catalog

import net.dodian.uber.game.shop.ShopPlugin
import net.dodian.uber.game.shop.ShopId
import net.dodian.uber.game.shop.shop

object JatixHerbloreStoreShop : ShopPlugin {
    override val definition =
        shop(shopId = ShopId.JATIX_HERBLORE_STORE) {
            sellModifier = 2
            buyModifier = 2
            item(233, 100)
            item(229, 10000)
            item(11877, 100)
            item(227, 10000)
            item(11879, 100)
        }.definition
}

