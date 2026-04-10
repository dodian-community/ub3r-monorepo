package net.dodian.uber.game.content.shop.plugin

import net.dodian.uber.game.content.shop.ShopDefinition
import net.dodian.uber.game.content.shop.ShopStockItem

class ShopPluginBuilder(
    private val name: String,
    private val shopId: Int,
) {
    var buyModifier: Int = 1
    var sellModifier: Int = 1
    var currencyItemId: Int = 995
    var buyPriceMultiplier: Double = 1.0
    var requiresPremium: Boolean = false

    private val stock = mutableListOf<ShopStockItem>()
    private val slotBuyPriceOverrides = linkedMapOf<Int, Int>()

    fun item(itemId: Int, amount: Int) {
        require(itemId >= 0) { "Shop item ids must be non-negative." }
        require(amount >= 0) { "Shop stock amounts must be non-negative." }
        stock += ShopStockItem(itemId = itemId, amount = amount)
    }

    fun buyPrice(slot: Int, price: Int) {
        require(slot >= 0) { "Shop price override slots must be non-negative." }
        require(price >= 0) { "Shop price overrides must be non-negative." }
        slotBuyPriceOverrides[slot] = price
    }

    fun items(vararg entries: Pair<Int, Int>) {
        for ((itemId, amount) in entries) {
            item(itemId = itemId, amount = amount)
        }
    }

    fun build(): ShopDefinition =
        ShopDefinition(
            id = shopId,
            name = name,
            sellModifier = sellModifier,
            buyModifier = buyModifier,
            stock = stock.toList(),
            currencyItemId = currencyItemId,
            buyPriceMultiplier = buyPriceMultiplier,
            slotBuyPriceOverrides = slotBuyPriceOverrides.toMap(),
            requiresPremium = requiresPremium,
        )
}

fun shopPlugin(name: String, shopId: Int, block: ShopPluginBuilder.() -> Unit): ShopPlugin =
    object : ShopPlugin {
        override val definition: ShopDefinition = ShopPluginBuilder(name = name, shopId = shopId).apply(block).build()
    }
