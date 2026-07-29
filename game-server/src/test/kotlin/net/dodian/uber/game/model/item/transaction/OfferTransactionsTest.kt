package net.dodian.uber.game.model.item.transaction

import net.dodian.uber.game.Server
import net.dodian.uber.game.api.plugin.social.ExchangeKind
import net.dodian.uber.game.engine.loop.GameThreadContext
import net.dodian.uber.game.item.ItemManager
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Item
import net.dodian.uber.game.social.exchange.ExchangeRuntime
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OfferTransactionsTest {
    private var previousItemManager: ItemManager? = null

    @BeforeEach
    fun setUpItemManager() {
        GameThreadContext.bindCurrentThread()
        previousItemManager = Server.itemManager
        Server.itemManager = ItemManager(
            definitionLoader = {
                mapOf(
                    100 to item(100, stackable = false),
                    200 to item(200, stackable = false),
                )
            },
            globalSpawnBootstrap = {},
        )
    }

    @AfterEach
    fun restoreItemManager() {
        GameThreadContext.clearBindingForTests()
        Server.itemManager = previousItemManager
    }

    @Test
    fun `trade projection exchanges reserved items without touching live state`() {
        val (first, second) = pair(ExchangeKind.TRADE)
        put(first, 0, 100)
        put(second, 0, 200)
        assertTrue(ExchangeRuntime.reserve(first, 0, 100, 1))
        assertTrue(ExchangeRuntime.reserve(second, 0, 200, 1))

        val projection = OfferTransactions.projectReservedTrade(first, second)

        assertNotNull(projection)
        assertEquals(101, first.playerItems[0])
        assertEquals(201, second.playerItems[0])
        assertEquals(201, projection!!.firstAfter.itemIds[0])
        assertEquals(101, projection.secondAfter.itemIds[0])
    }

    @Test
    fun `trade projection fails atomically when recipient has no capacity`() {
        val (first, second) = pair(ExchangeKind.TRADE)
        put(first, 0, 100)
        second.playerItems.indices.forEach { put(second, it, 200) }
        assertTrue(ExchangeRuntime.reserve(first, 0, 100, 1))

        assertNull(OfferTransactions.projectReservedTrade(first, second))
        assertEquals(101, first.playerItems[0])
        assertTrue(second.playerItems.all { it == 201 })
    }

    @Test
    fun `duel payout transfers loser stake and leaves winner stake in place`() {
        val (winner, loser) = pair(ExchangeKind.DUEL)
        put(winner, 0, 100)
        put(loser, 0, 200)
        assertTrue(ExchangeRuntime.reserve(winner, 0, 100, 1))
        assertTrue(ExchangeRuntime.reserve(loser, 0, 200, 1))

        val projection = OfferTransactions.projectReservedDuelPayout(winner, loser)

        assertNotNull(projection)
        assertEquals(101, projection!!.firstAfter.itemIds[0])
        assertTrue(projection.firstAfter.itemIds.contains(201))
        assertFalse(projection.secondAfter.itemIds.contains(201))
        assertEquals(201, loser.playerItems[0])
    }

    @Test
    fun `publishing rejects a changed base inventory`() {
        val (first, second) = pair(ExchangeKind.TRADE)
        put(first, 0, 100)
        put(second, 0, 200)
        assertTrue(ExchangeRuntime.reserve(first, 0, 100, 1))
        assertTrue(ExchangeRuntime.reserve(second, 0, 200, 1))
        val projection = requireNotNull(OfferTransactions.projectReservedTrade(first, second))
        first.playerItemsN[0] = 2

        assertFalse(OfferTransactions.publishProjection(first, second, projection))
        assertEquals(101, first.playerItems[0])
        assertEquals(2, first.playerItemsN[0])
        assertEquals(201, second.playerItems[0])
    }

    private fun pair(kind: ExchangeKind): Pair<Client, Client> {
        val first = Client(null, 1).apply { dbId = 10 }
        val second = Client(null, 2).apply { dbId = 20 }
        ExchangeRuntime.create(kind, first, second)
        return first to second
    }

    private fun put(client: Client, slot: Int, id: Int) {
        client.playerItems[slot] = id + 1
        client.playerItemsN[slot] = 1
    }

    private fun item(id: Int, stackable: Boolean) = Item(
        id = id,
        name = "test-$id",
        slot = -1,
        standAnim = 0,
        walkAnim = 0,
        runAnim = 0,
        attackAnim = 0,
        shopSellValue = 0,
        shopBuyValue = 0,
        bonuses = IntArray(12),
        stackable = stackable,
        noteable = false,
        tradeable = true,
        twoHanded = false,
        full = false,
        mask = false,
        premium = false,
        examine = "test",
        alchemy = 0,
    )
}
