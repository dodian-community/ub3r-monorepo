package net.dodian.uber.game.shop.catalog

import net.dodian.uber.game.shop.ShopPlugin
import net.dodian.uber.game.shop.ShopId
import net.dodian.uber.game.shop.shop

object WeaponAndArmorShop : ShopPlugin {
    override val definition =
        shop(shopId = ShopId.WEAPON_AND_ARMOR) {
            sellModifier = 2
            buyModifier = 2
            buyPriceMultiplier = 1.5
            item(1155, 100)
            item(1153, 100)
            item(1157, 100)
            item(1165, 100)
            item(1117, 100)
            item(1115, 100)
            item(1119, 100)
            item(1125, 100)
            item(1075, 100)
            item(1067, 100)
            item(1069, 100)
            item(1077, 100)
            item(1087, 100)
            item(1081, 100)
            item(1083, 100)
            item(1089, 100)
            item(1189, 100)
            item(1191, 100)
            item(1193, 100)
            item(1195, 100)
            item(1321, 100)
            item(1323, 100)
            item(1325, 100)
            item(1327, 100)
            item(1375, 100)
            item(1363, 100)
            item(1365, 100)
            item(1367, 100)
            item(1307, 100)
            item(1309, 100)
            item(1311, 100)
            item(1313, 100)
        }.definition
}

