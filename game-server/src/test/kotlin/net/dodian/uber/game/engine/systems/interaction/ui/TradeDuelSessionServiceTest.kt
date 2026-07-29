package net.dodian.uber.game.engine.systems.interaction.ui

import net.dodian.uber.game.Server
import net.dodian.uber.game.api.plugin.social.ExchangeCommandResult
import net.dodian.uber.game.api.plugin.social.ExchangeKind
import net.dodian.uber.game.engine.loop.GameThreadContext
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.item.ItemManager
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Item
import net.dodian.uber.game.model.item.transaction.OfferTransactions
import net.dodian.uber.game.social.exchange.ExchangeOfferService
import net.dodian.uber.game.social.exchange.ExchangeRuntime
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
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
        (PlayerRegistry.players[1] as? Client)?.let {
            PlayerSocialApproachService.cancel(it)
            ExchangeRuntime.remove(it)
        }
        (PlayerRegistry.players[2] as? Client)?.let {
            PlayerSocialApproachService.cancel(it)
            ExchangeRuntime.remove(it)
        }
        PlayerRegistry.players[1] = null
        PlayerRegistry.players[2] = null
        GameThreadContext.clearBindingForTests()
        Server.itemManager = previousItemManager
    }

    @Test
    fun `adjacent reciprocal trade opens without staging movement`() {
        val first = client(1, 10, "first").apply { position.moveTo(3200, 3200, 0) }
        val second = client(2, 20, "second").apply { position.moveTo(3201, 3200, 0) }
        PlayerRegistry.players[1] = first
        PlayerRegistry.players[2] = second

        TradeDuelSessionService.requestTrade(first, 2)
        TradeDuelSessionService.requestTrade(second, 1)

        assertTrue(first.inTrade)
        assertTrue(second.inTrade)
        assertNotNull(ExchangeRuntime.session(first, second, ExchangeKind.TRADE))
        assertFalse(first.hasMovementRoute())
        assertFalse(second.hasMovementRoute())
    }

    @Test
    fun `offering reserves without removing the live inventory`() {
        val (first, second) = tradePair()
        first.playerItems[0] = 996
        first.playerItemsN[0] = 50

        ExchangeOfferService.offerTrade(first, 995, 0, 10)

        assertEquals(50, first.playerItemsN[0])
        assertEquals(10, ExchangeRuntime.offers(first).single().amount)
        assertEquals(40, ExchangeRuntime.availableAt(first, 0, 995))
        assertTrue(ExchangeRuntime.offers(second).isEmpty())
    }

    @Test
    fun `withdraw one releases exactly one from a larger offer`() {
        val (first, _) = tradePair()
        first.playerItems[0] = 996
        first.playerItemsN[0] = 5
        assertTrue(ExchangeRuntime.reserve(first, 0, 995, 5))

        ExchangeOfferService.withdrawTrade(first, 995, 0, 1)

        assertEquals(5, first.playerItemsN[0])
        assertEquals(4, ExchangeRuntime.offers(first).single().amount)
        assertEquals(1, ExchangeRuntime.availableAt(first, 0, 995))
    }

    @Test
    fun `ordinary inventory transactions are blocked throughout trade negotiation`() {
        val (first, _) = tradePair()
        first.playerItems[0] = 996
        first.playerItemsN[0] = 5
        assertTrue(ExchangeRuntime.reserve(first, 0, 995, 4))

        first.deleteItem(995, 0, 2)

        assertEquals(5, first.playerItemsN[0])
        first.deleteItem(995, 0, 1)
        assertEquals(5, first.playerItemsN[0])
        assertEquals(4, ExchangeRuntime.offers(first).single().amount)
    }

    @Test
    fun `active duel can consume unreserved supplies but never its reserved stake`() {
        val first = client(1, 10, "first")
        val second = client(2, 20, "second")
        PlayerRegistry.players[1] = first
        PlayerRegistry.players[2] = second
        val session = ExchangeRuntime.create(ExchangeKind.DUEL, first, second)
        first.playerItems[0] = 996
        first.playerItemsN[0] = 5
        assertTrue(ExchangeRuntime.reserve(first, 0, 995, 4))
        assertTrue(session.accept(ExchangeRuntime.participant(first), session.revision) is ExchangeCommandResult.Applied)
        assertTrue(session.accept(ExchangeRuntime.participant(second), session.revision) is ExchangeCommandResult.Applied)
        assertTrue(session.accept(ExchangeRuntime.participant(first), session.revision) is ExchangeCommandResult.Applied)
        assertTrue(session.accept(ExchangeRuntime.participant(second), session.revision) is ExchangeCommandResult.Applied)
        assertTrue(session.beginActive() is ExchangeCommandResult.Applied)

        first.deleteItem(995, 0, 1)
        assertEquals(4, first.playerItemsN[0])
        first.deleteItem(995, 0, 1)
        assertEquals(4, first.playerItemsN[0])
        assertEquals(4, ExchangeRuntime.offers(first).single().amount)
    }

    @Test
    fun `trade projection conserves items without publishing early`() {
        val (first, second) = tradePair()
        first.playerItems[0] = 996
        first.playerItemsN[0] = 50
        second.playerItems[0] = 101
        second.playerItemsN[0] = 1
        second.playerItems[1] = 101
        second.playerItemsN[1] = 1
        assertTrue(ExchangeRuntime.reserve(first, 0, 995, 50))
        assertTrue(ExchangeRuntime.reserve(second, 0, 100, 2))

        val projection = OfferTransactions.projectReservedTrade(first, second)

        assertNotNull(projection)
        assertEquals(50, first.playerItemsN[0])
        assertEquals(2, second.playerItems.count { it == 101 })
        assertEquals(2, projection!!.firstAfter.itemIds.count { it == 101 })
        assertEquals(50, projection.secondAfter.amounts.single { it > 0 })
    }

    @Test
    fun `cancellation only discards reservations`() {
        val (first, second) = tradePair()
        first.playerItems[0] = 996
        first.playerItemsN[0] = 10
        assertTrue(ExchangeRuntime.reserve(first, 0, 995, 10))

        assertTrue(TradeDuelSessionService.cancelTrade(first, second))

        assertEquals(10, first.playerItemsN[0])
        assertTrue(ExchangeRuntime.offers(first).isEmpty())
    }

    @Test
    fun `second trade confirmation advances the session to settlement`() {
        val (first, second) = tradePair()
        val session = requireNotNull(ExchangeRuntime.session(first, second, ExchangeKind.TRADE))

        assertTrue(session.accept(ExchangeRuntime.participant(first), session.revision) is ExchangeCommandResult.Applied)
        first.tradeConfirmed = true
        assertTrue(session.accept(ExchangeRuntime.participant(second), session.revision) is ExchangeCommandResult.Applied)
        second.tradeConfirmed = true
        assertTrue(TradeDuelSessionService.confirmationsCurrent(first, second))

        assertTrue(session.accept(ExchangeRuntime.participant(first), session.revision) is ExchangeCommandResult.Applied)
        first.tradeConfirmed2 = true
        assertTrue(session.accept(ExchangeRuntime.participant(second), session.revision) is ExchangeCommandResult.Applied)
        second.tradeConfirmed2 = true

        assertFalse(TradeDuelSessionService.confirmationsCurrent(first, second))
        assertTrue(TradeDuelSessionService.settlementCurrent(first, second))
    }

    private fun tradePair(): Pair<Client, Client> {
        val first = client(1, 10, "first").apply { inTrade = true }
        val second = client(2, 20, "second").apply { inTrade = true }
        PlayerRegistry.players[1] = first
        PlayerRegistry.players[2] = second
        TradeDuelSessionService.beginTradeSession(first, second)
        return first to second
    }

    private fun client(slot: Int, dbId: Int, name: String) =
        Client(null, slot).apply { this.dbId = dbId; playerName = name }

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
