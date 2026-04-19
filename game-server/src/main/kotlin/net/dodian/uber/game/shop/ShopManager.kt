package net.dodian.uber.game.shop

class ShopManager {
    init {
        reloadShops()
    }

    fun DiscountItem(ShopID: Int, ArrayID: Int) {
        ShopItemsN[ShopID][ArrayID] -= 1
        if (ShopItemsN[ShopID][ArrayID] <= 0 && ArrayID >= ShopItemsStandard[ShopID]) {
            ShopItemsN[ShopID][ArrayID] = 0
            ResetItem(ShopID, ArrayID)
        }
    }

    fun ResetItem(ShopID: Int, ArrayID: Int) {
        ShopItems[ShopID][ArrayID] = 0
        ShopItemsN[ShopID][ArrayID] = 0
        ShopItemsDelay[ShopID] = 0
    }

    @Suppress("UNUSED_PARAMETER")
    fun loadShops(FileName: String): Boolean = reloadShops()

    @Synchronized
    fun reloadShops(): Boolean {
        for (i in 0 until MaxShops) {
            for (j in 0 until MaxShopItems) {
                ResetItem(i, j)
                ShopItemsSN[i][j] = 0
            }
            ShopItemsStandard[i] = 0
            ShopSModifier[i] = 0
            ShopBModifier[i] = 0
            ShopName[i] = ""
        }
        var loadedShops = 0
        for (definition in ShopCatalog.all()) {
            require(definition.id in 0 until MaxShops) {
                "Shop ${definition.id} is outside the configured shop bounds (max=${MaxShops - 1})."
            }
            require(definition.stock.size <= MaxShopItems) {
                "Shop ${definition.id} exceeds MaxShopItems=$MaxShopItems with ${definition.stock.size} items."
            }
            ShopName[definition.id] = definition.name
            ShopSModifier[definition.id] = definition.sellModifier
            ShopBModifier[definition.id] = definition.buyModifier
            definition.stock.forEachIndexed { index, item ->
                ShopItems[definition.id][index] = item.itemId + 1
                ShopItemsN[definition.id][index] = item.amount
                ShopItemsSN[definition.id][index] = item.amount
                ShopItemsStandard[definition.id]++
            }
            loadedShops++
        }
        TotalShops = loadedShops
        return true
    }

    companion object {
        @JvmField var MaxShops: Int = 101
        @JvmField var MaxShopItems: Int = 40
        @JvmField var MaxShowDelay: Int = 100
        @JvmField var TotalShops: Int = 0

        @JvmField var ShopItems: Array<IntArray> = Array(MaxShops) { IntArray(MaxShopItems) }
        @JvmField var ShopItemsN: Array<IntArray> = Array(MaxShops) { IntArray(MaxShopItems) }
        @JvmField var ShopItemsDelay: IntArray = IntArray(MaxShops)
        @JvmField var ShopItemsSN: Array<IntArray> = Array(MaxShops) { IntArray(MaxShopItems) }
        @JvmField var ShopItemsStandard: IntArray = IntArray(MaxShops)
        @JvmField var ShopName: Array<String> = Array(MaxShops) { "" }
        @JvmField var ShopSModifier: IntArray = IntArray(MaxShops)
        @JvmField var ShopBModifier: IntArray = IntArray(MaxShops)

        @JvmStatic
        fun resetAnItem(ShopID: Int, ArrayID: Int) {
            ShopItems[ShopID][ArrayID] = -1
            ShopItemsN[ShopID][ArrayID] = 0
            ShopItemsDelay[ShopID] = 0
        }

        @JvmStatic
        fun findDefaultItem(shopId: Int, id: Int): Boolean {
            if (ShopRulesService.isDefaultStockItem(shopId, id)) {
                return true
            }
            for (i in 0 until ShopItemsStandard[shopId]) {
                if (ShopItems[shopId][i] - 1 == id) {
                    return true
                }
            }
            return false
        }
    }
}

