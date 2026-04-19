package net.dodian.uber.game.shop


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

object ShopCatalog {
    @JvmStatic
    fun all(): List<ShopDefinition> = ShopCatalogRegistry.all()

    @JvmStatic
    fun find(id: Int): ShopDefinition? = ShopCatalogRegistry.find(id)
}


