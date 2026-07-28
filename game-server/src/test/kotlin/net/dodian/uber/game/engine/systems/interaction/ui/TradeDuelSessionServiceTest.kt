package net.dodian.uber.game.engine.systems.interaction.ui

import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.loop.GameThreadContext
import net.dodian.uber.game.item.ItemManager
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.GameItem
import net.dodian.uber.game.model.item.Item
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TradeDuelSessionServiceTest {
    private var previousItemManager: ItemManager? = null

    @BeforeEach
    fun setUp() {
        GameThreadContext.bindCurrentThread()
        previousItemManager = Server.itemManager
        Server.itemManager = ItemManager(
            definitionLoader = {
                mapOf(
                    100 to item(100, false),
                    995 to item(995, true),
                )
            },
            globalSpawnBootstrap = {},
        )
    }

    @AfterEach
    fun tearDown() {
        (PlayerRegistry.players[1] as? Client)?.let(PlayerSocialApproachService::cancel)
        (PlayerRegistry.players[2] as? Client)?.let(PlayerSocialApproachService::cancel)
        PlayerRegistry.players[1] = null
        PlayerRegistry.players[2] = null
        GameThreadContext.clearBindingForTests()
        Server.itemManager = previousItemManager
    }

    @Test
    fun `adjacent reciprocal trade opens without staging movement`() {
        val first = Client(null, 1).apply {
            dbId = 10
            playerName = "first"
            position.moveTo(3200, 3200, 0)
        }
        val second = Client(null, 2).apply {
            dbId = 20
            playerName = "second"
            position.moveTo(3201, 3200, 0)
        }
        PlayerRegistry.players[1] = first
        PlayerRegistry.players[2] = second

        TradeDuelSessionService.requestTrade(first, 2)
        TradeDuelSessionService.requestTrade(second, 1)

        assertTrue(first.inTrade)
        assertTrue(second.inTrade)
        assertEquals(0, first.newWalkCmdSteps)
        assertEquals(first.wQueueReadPtr, first.wQueueWritePtr)
        assertEquals(0, second.newWalkCmdSteps)
        assertEquals(second.wQueueReadPtr, second.wQueueWritePtr)
    }

    @Test
    fun `offer mutation invalidates both confirmations`() {
        val (first, second) = tradePair()
        assertTrue(TradeDuelSessionService.recordStageOneConfirmation(first, second))
        assertTrue(TradeDuelSessionService.recordStageOneConfirmation(second, first))
        assertTrue(TradeDuelSessionService.confirmationsCurrent(first, second))

        TradeDuelSessionService.offerChanged(first, second)

        assertFalse(first.tradeConfirmed)
        assertFalse(second.tradeConfirmed)
        assertFalse(TradeDuelSessionService.confirmationsCurrent(first, second))
    }

    @Test
    fun `settlement conserves items and commits a session once`() {
        val (first, second) = tradePair()
        first.offeredItems += GameItem(995, 50)
        second.offeredItems += GameItem(100, 2)
        confirmBoth(first, second)

        assertTrue(TradeDuelSessionService.settleTrade(first, second))
        assertEquals(50, second.playerItemsN.single { it > 0 })
        assertEquals(2, first.playerItems.count { it == 101 })
        assertTrue(first.offeredItems.isEmpty())
        assertTrue(second.offeredItems.isEmpty())

        assertFalse(TradeDuelSessionService.settleTrade(first, second))
        assertEquals(50, second.playerItemsN.single { it > 0 })
        assertEquals(2, first.playerItems.count { it == 101 })
    }

    @Test
    fun `stale partner session rejects settlement without mutation`() {
        val (first, second) = tradePair()
        first.offeredItems += GameItem(995, 10)
        second.offeredItems += GameItem(100, 1)
        confirmBoth(first, second)
        second.tradeSessionId++

        assertFalse(TradeDuelSessionService.settleTrade(first, second))
        assertEquals(10, first.offeredItems.single().amount)
        assertEquals(1, second.offeredItems.single().amount)
        assertTrue(first.playerItems.all { it == 0 })
        assertTrue(second.playerItems.all { it == 0 })
    }

    @Test
    fun `cancellation refunds a reciprocal session at most once`() {
        val (first, second) = tradePair()
        first.offeredItems += GameItem(995, 10)
        second.offeredItems += GameItem(100, 2)

        assertTrue(TradeDuelSessionService.cancelTrade(first, second))
        assertEquals(10, first.playerItemsN.single { it > 0 })
        assertEquals(2, second.playerItems.count { it == 101 })
        assertTrue(first.offeredItems.isEmpty())
        assertTrue(second.offeredItems.isEmpty())

        assertFalse(TradeDuelSessionService.cancelTrade(first, second))
        assertEquals(10, first.playerItemsN.single { it > 0 })
        assertEquals(2, second.playerItems.count { it == 101 })
    }

    @Test
    fun `disconnect rejects settlement without consuming either offer`() {
        val (first, second) = tradePair()
        first.offeredItems += GameItem(995, 10)
        second.offeredItems += GameItem(100, 1)
        confirmBoth(first, second)
        second.disconnected = true

        assertFalse(TradeDuelSessionService.settleTrade(first, second))
        assertEquals(10, first.offeredItems.single().amount)
        assertEquals(1, second.offeredItems.single().amount)
    }

    @Test
    fun `logout cancellation can refund only the departing side`() {
        val (first, second) = tradePair()
        first.offeredItems += GameItem(995, 10)
        second.offeredItems += GameItem(100, 1)

        assertTrue(TradeDuelSessionService.cancelTrade(first, null))
        assertEquals(10, first.playerItemsN.single { it > 0 })
        assertTrue(first.offeredItems.isEmpty())
        assertEquals(1, second.offeredItems.single().amount)
        assertTrue(second.playerItems.all { it == 0 })
    }

    @Test
    fun `recipient stack overflow rejects settlement atomically`() {
        val (first, second) = tradePair()
        first.playerItems[0] = 996
        first.playerItemsN[0] = first.maxItemAmount
        second.offeredItems += GameItem(995, 1)
        confirmBoth(first, second)

        assertFalse(TradeDuelSessionService.settleTrade(first, second))
        assertEquals(first.maxItemAmount, first.playerItemsN[0])
        assertEquals(1, second.offeredItems.single().amount)
    }

    private fun tradePair(): Pair<Client, Client> {
        val first = Client(null, 1).apply { inTrade = true; dbId = 10; playerName = "first" }
        val second = Client(null, 2).apply { inTrade = true; dbId = 20; playerName = "second" }
        TradeDuelSessionService.beginTradeSession(first, second)
        return first to second
    }

    private fun confirmBoth(first: Client, second: Client) {
        assertTrue(TradeDuelSessionService.recordStageOneConfirmation(first, second))
        assertTrue(TradeDuelSessionService.recordStageOneConfirmation(second, first))
        first.tradeConfirmed2 = true
        second.tradeConfirmed2 = true
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
