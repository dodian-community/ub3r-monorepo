package net.dodian.uber.game.shop.catalog

import net.dodian.uber.game.shop.ShopPlugin
import net.dodian.uber.game.shop.ShopId
import net.dodian.uber.game.shop.shop

object PremiumMemberShop : ShopPlugin {
    override val definition =
        shop(shopId = ShopId.PREMIUM_MEMBER_SHOP) {
            sellModifier = 2
            buyModifier = 2
            requiresPremium = true
            item(2643, 100)
            item(1419, 100)
            item(2583, 100)
            item(2585, 100)
            item(2587, 100)
            item(2589, 100)
            item(2591, 100)
            item(2593, 100)
            item(2595, 100)
            item(2597, 100)
            item(2599, 100)
            item(2601, 100)
            item(2603, 100)
            item(2605, 100)
            item(2607, 100)
            item(2609, 100)
            item(2611, 100)
            item(2613, 100)
            item(2615, 100)
            item(2617, 100)
            item(2619, 100)
            item(2621, 100)
        }.definition
}

