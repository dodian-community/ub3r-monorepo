package net.dodian.uber.game.content.shop

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

private class ShopDefinitionBuilder(
    private val id: Int,
    private val name: String,
) {
    var sellModifier: Int = 1
    var buyModifier: Int = 1
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

    fun build(): ShopDefinition =
        ShopDefinition(
            id = id,
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

private fun shop(id: Int, name: String, block: ShopDefinitionBuilder.() -> Unit): ShopDefinition =
    ShopDefinitionBuilder(id = id, name = name).apply(block).build()

object ShopDefinitions {
    private val definitions: Map<Int, ShopDefinition> =
        listOf(
            shop(id = 3, name = "General Store") {
                sellModifier = 1
                buyModifier = 1
                item(590, 2)
                item(2347, 5)
                item(946, 5)
                item(1351, 10)
                item(1265, 10)
                item(1755, 5)
                item(4155, 10)
                item(1735, 10)
                item(1925, 1337)
            },
            shop(id = 9, name = "Aubury's Magic Store") {
                sellModifier = 2
                buyModifier = 2
                buyPriceMultiplier = 1.5
                item(2415, 10)
                item(2416, 10)
                item(2417, 10)
                item(4089, 10)
                item(4091, 10)
                item(4093, 10)
                item(4095, 10)
                item(4097, 10)
            },
            shop(id = 10, name = "Weapon and Armor") {
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
            },
            shop(id = 11, name = "Range store") {
                sellModifier = 2
                buyModifier = 2
                buyPriceMultiplier = 1.5
                item(839, 10)
                item(841, 10)
                item(843, 10)
                item(845, 10)
                item(847, 10)
                item(849, 10)
                item(1063, 10)
                item(1065, 10)
                item(1095, 10)
                item(1099, 10)
                item(1129, 10)
                item(1135, 10)
                item(882, 10000)
                item(884, 5000)
                item(886, 2000)
            },
            shop(id = 15, name = "Slayer Shop") {
                sellModifier = 2
                buyModifier = 2
                item(4170, 1)
                item(4156, 1)
                item(4158, 1)
                item(4160, 1)
                item(5553, 1)
                item(5554, 1)
                item(5555, 1)
                item(5556, 1)
                item(5557, 1)
                item(6585, 100)
            },
            shop(id = 18, name = "Fishing Store") {
                sellModifier = 2
                buyModifier = 2
                item(303, 1000)
                item(301, 1000)
                item(311, 1000)
                item(309, 1000)
                item(317, 0)
            },
            shop(id = 20, name = "Premium Member Shop") {
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
            },
            shop(id = 25, name = "Crafting store") {
                sellModifier = 2
                buyModifier = 2
                item(1733, 10000)
                item(1734, 25000)
                item(1592, 10000)
                item(1597, 10000)
                item(1595, 10000)
                item(11065, 10000)
            },
            shop(id = 34, name = "Cape Shop") {
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
            },
            shop(id = 39, name = "Jatix herblore Store") {
                sellModifier = 2
                buyModifier = 2
                item(233, 100)
                item(229, 10000)
                item(11877, 100)
                item(227, 10000)
                item(11879, 100)
            },
            shop(id = 40, name = "Fishing supplies") {
                sellModifier = 2
                buyModifier = 2
                item(303, 1000)
                item(301, 1000)
                item(311, 1000)
                item(309, 1000)
                item(317, 0)
            },
            shop(id = 55, name = "Christmas Event Store") {
                sellModifier = 2
                buyModifier = 3
                currencyItemId = 11997
                buyPrice(0, 8000)
                buyPrice(1, 12000)
                buyPrice(2, 12000)
                buyPrice(3, 4000)
                buyPrice(4, 4000)
                buyPrice(5, 25000)
                buyPrice(6, 15000)
                item(12887, 100)
                item(12888, 100)
                item(12889, 100)
                item(12890, 100)
                item(12891, 100)
                item(10507, 100)
                item(12854, 1000)
            },
        ).associateBy { it.id }

    @JvmStatic
    fun all(): List<ShopDefinition> = definitions.values.sortedBy { it.id }

    @JvmStatic
    fun find(id: Int): ShopDefinition? = definitions[id]
}
