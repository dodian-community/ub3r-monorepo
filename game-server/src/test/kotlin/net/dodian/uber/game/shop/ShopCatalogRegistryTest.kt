package net.dodian.uber.game.shop
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ShopCatalogRegistryTest {
    @AfterEach
    fun tearDown() {
        ShopCatalogRegistry.resetForTests()
    }

    @Test
    fun `shop plugin DSL builds immutable definition`() {
        val def =
            shop("General", shopId = 3) {
                buyModifier = 1
                sellModifier = 1
                currencyItemId = 11997
                buyPriceMultiplier = 1.5
                requiresPremium = true
                buyPrice(slot = 0, price = 8000)
                item(590, 2)
            }.definition

        assertEquals(3, def.id)
        assertEquals(1, def.buyModifier)
        assertEquals(590, def.stock.first().itemId)
        assertEquals(11997, def.currencyItemId)
        assertEquals(1.5, def.buyPriceMultiplier)
        assertEquals(8000, def.slotBuyPriceOverrides[0])
    }

    @Test
    fun `shop registry rejects duplicate shop ids`() {
        ShopCatalogRegistry.resetForTests()
        ShopCatalogRegistry.register(shop("A", shopId = 99) { item(590, 1) })
        ShopCatalogRegistry.register(shop("B", shopId = 99) { item(591, 1) })

        assertThrows(IllegalArgumentException::class.java) {
            ShopCatalogRegistry.bootstrap()
        }
    }

    @Test
    fun `shop registry bootstraps from generated module index`() {
        ShopCatalogRegistry.resetForTests()
        ShopCatalogRegistry.bootstrap()
        assertTrue(ShopCatalogRegistry.all().isNotEmpty())
    }

    @Test
    fun `shop registry rejects invalid ids and slot price mismatches`() {
        ShopCatalogRegistry.resetForTests()
        ShopCatalogRegistry.register(
            shop("Broken", shopId = -1) {
                item(590, 1)
            },
        )
        assertThrows(IllegalArgumentException::class.java) {
            ShopCatalogRegistry.bootstrap()
        }

        ShopCatalogRegistry.resetForTests()
        ShopCatalogRegistry.register(
            shop("BrokenSlot", shopId = 77) {
                item(590, 1)
                buyPrice(slot = 4, price = 50)
            },
        )
        assertThrows(IllegalArgumentException::class.java) {
            ShopCatalogRegistry.bootstrap()
        }
    }
}


