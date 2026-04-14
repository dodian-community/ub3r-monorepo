package net.dodian.uber.game.shop.catalog

import net.dodian.uber.game.shop.ShopPlugin
import net.dodian.uber.game.shop.ShopId
import net.dodian.uber.game.shop.shop

object CraftingStoreShop : ShopPlugin {
    override val definition =
        shop(shopId = ShopId.CRAFTING_STORE) {
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

