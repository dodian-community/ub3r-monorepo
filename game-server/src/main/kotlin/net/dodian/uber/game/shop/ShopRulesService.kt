package net.dodian.uber.game.shop

import kotlin.math.floor
import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.player.Client

object ShopRulesService {
    @JvmStatic
    fun accessDeniedMessage(client: Client, shopId: Int): String? {
        val definition = ShopCatalog.find(shopId) ?: return null
        return if (definition.requiresPremium && !client.premium) {
            "You need to be a premium member to access this shop."
        } else {
            null
        }
    }

    @JvmStatic
    fun currencyItemId(shopId: Int): Int = ShopCatalog.find(shopId)?.currencyItemId ?: 995

    @JvmStatic
    fun buyPrice(shopId: Int, itemId: Int, slot: Int): Int {
        val definition = ShopCatalog.find(shopId)
        val slotOverride = definition?.slotBuyPriceOverrides?.get(slot)
        if (slotOverride != null) {
            return slotOverride
        }
        val basePrice = Server.itemManager?.getShopSellValue(itemId) ?: 0
        val multiplier = definition?.buyPriceMultiplier ?: 1.0
        return floor(basePrice * multiplier).toInt().coerceAtLeast(0)
    }

    @JvmStatic
    fun sellPrice(itemId: Int): Int = (Server.itemManager?.getShopBuyValue(itemId) ?: 0).coerceAtLeast(0)

    @JvmStatic
    fun canSellItemToShop(shopId: Int, itemId: Int): Boolean {
        val definition = ShopCatalog.find(shopId) ?: return false
        if (itemId < 0) return false
        if (isShopCurrency(itemId)) return false
        if (sellPrice(itemId) <= 0) return false
        return when {
            definition.buyModifier > 2 -> false
            definition.buyModifier == 2 -> isDefaultStockItem(shopId, itemId)
            else -> true
        }
    }

    @JvmStatic
    fun isDefaultStockItem(shopId: Int, itemId: Int): Boolean =
        ShopCatalog.find(shopId)?.stock?.any { it.itemId == itemId } == true

    @JvmStatic
    fun isShopCurrency(itemId: Int): Boolean = ShopCatalog.all().any { it.currencyItemId == itemId }
}

