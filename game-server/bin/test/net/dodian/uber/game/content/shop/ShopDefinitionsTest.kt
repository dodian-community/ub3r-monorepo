package net.dodian.uber.game.content.shop

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.item.Item
import net.dodian.uber.game.model.item.ItemManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ShopDefinitionsTest {
    private var previousItemManager: ItemManager? = null

    @BeforeEach
    fun setUp() {
        previousItemManager = Server.itemManager
        Server.itemManager =
            ItemManager(
                definitionLoader = {
                    mapOf(
                        2415 to testItem(id = 2415, name = "Saradomin cape", shopSellValue = 100, shopBuyValue = 40),
                        12887 to testItem(id = 12887, name = "Festive item", shopSellValue = 1, shopBuyValue = 1),
                        590 to testItem(id = 590, name = "Tinderbox", shopSellValue = 2, shopBuyValue = 1),
                        2000 to testItem(id = 2000, name = "Test sellable", shopSellValue = 10, shopBuyValue = 5),
                    )
                },
                globalSpawnBootstrap = {},
            )
    }

    @AfterEach
    fun tearDown() {
        Server.itemManager = previousItemManager
    }

    @Test
    fun `definitions preserve migrated metadata`() {
        val premiumShop = ShopDefinitions.find(20) ?: error("premium shop missing")
        val eventShop = ShopDefinitions.find(55) ?: error("event shop missing")

        assertTrue(premiumShop.requiresPremium)
        assertEquals(22, premiumShop.stock.size)
        assertEquals(11997, eventShop.currencyItemId)
        assertEquals(25000, eventShop.slotBuyPriceOverrides[5])
        assertEquals(12, ShopDefinitions.all().size)
    }

    @Test
    fun `shop manager reload hydrates runtime arrays from definitions`() {
        val manager = ShopManager()

        assertEquals("General Store", ShopManager.ShopName[3])
        assertEquals(590 + 1, ShopManager.ShopItems[3][0])
        assertEquals(2, ShopManager.ShopItemsN[3][0])

        ShopManager.ShopItemsN[3][0] = 0
        manager.reloadShops()

        assertEquals(2, ShopManager.ShopItemsN[3][0])
        assertEquals(12887 + 1, ShopManager.ShopItems[55][0])
    }

    @Test
    fun `shop rules centralize special pricing and sell policies`() {
        assertEquals(150, ShopRulesService.buyPrice(shopId = 9, itemId = 2415, slot = 0))
        assertEquals(8000, ShopRulesService.buyPrice(shopId = 55, itemId = 12887, slot = 0))
        assertTrue(ShopRulesService.canSellItemToShop(shopId = 3, itemId = 2000))
        assertFalse(ShopRulesService.canSellItemToShop(shopId = 3, itemId = 995))
        assertFalse(ShopRulesService.canSellItemToShop(shopId = 55, itemId = 11997))
        assertFalse(ShopRulesService.canSellItemToShop(shopId = 55, itemId = 12887))
        assertTrue(ShopRulesService.canSellItemToShop(shopId = 9, itemId = 2415))
        assertFalse(ShopRulesService.canSellItemToShop(shopId = 9, itemId = 9999))
    }

    private fun testItem(id: Int, name: String, shopSellValue: Int, shopBuyValue: Int): Item =
        Item(
            id = id,
            slot = 0,
            standAnim = 808,
            walkAnim = 819,
            runAnim = 824,
            attackAnim = 806,
            shopSellValue = shopSellValue,
            shopBuyValue = shopBuyValue,
            bonuses = IntArray(12),
            stackable = false,
            noteable = false,
            tradeable = true,
            twoHanded = false,
            full = false,
            mask = false,
            premium = false,
            name = name,
            description = "test item",
            alchemy = 0,
        )
}

