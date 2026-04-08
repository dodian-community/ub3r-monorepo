package net.dodian.uber.game.content.shop

import net.dodian.uber.game.content.shop.plugin.ShopPluginRegistry
import net.dodian.uber.game.content.shop.plugin.shopPlugin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ShopPluginRegistryTest {
    @Test
    fun `shop plugin DSL builds immutable definition`() {
        val def =
            shopPlugin("General", shopId = 3) {
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
        ShopPluginRegistry.resetForTests()
        ShopPluginRegistry.register(shopPlugin("A", shopId = 99) { item(590, 1) })
        ShopPluginRegistry.register(shopPlugin("B", shopId = 99) { item(591, 1) })

        assertThrows(IllegalArgumentException::class.java) {
            ShopPluginRegistry.bootstrap()
        }
    }

    @Test
    fun `shop registry bootstraps from generated module index`() {
        ShopPluginRegistry.resetForTests()
        ShopPluginRegistry.bootstrap()
        assertTrue(ShopPluginRegistry.all().isNotEmpty())
    }
}
