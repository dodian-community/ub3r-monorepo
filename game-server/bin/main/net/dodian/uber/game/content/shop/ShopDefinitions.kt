package net.dodian.uber.game.content.shop

import net.dodian.uber.game.content.shop.plugin.ShopPluginRegistry

data class ShopStockItem(
    val itemId: Int,
    val amount: Int,
)

data class ShopDefinition(
    val id: Int,
    val name: String,
    val sellModifier: Int,
    val buyModifier: Int,
    val stock: List<ShopStockItem>,
    val currencyItemId: Int = 995,
    val buyPriceMultiplier: Double = 1.0,
    val slotBuyPriceOverrides: Map<Int, Int> = emptyMap(),
    val requiresPremium: Boolean = false,
)

object ShopDefinitions {
    @JvmStatic
    fun all(): List<ShopDefinition> = ShopPluginRegistry.all()

    @JvmStatic
    fun find(id: Int): ShopDefinition? = ShopPluginRegistry.find(id)
}
