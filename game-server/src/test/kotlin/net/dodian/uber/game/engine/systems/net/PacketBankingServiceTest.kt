package net.dodian.uber.game.engine.systems.net

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Item
import net.dodian.uber.game.model.item.ItemManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PacketBankingServiceTest {
    private var previousItemManager: ItemManager? = null

    @BeforeEach
    fun setUp() {
        previousItemManager = Server.itemManager
        Server.itemManager =
            ItemManager(
                definitionLoader = {
                    mapOf(
                        100 to itemDefinition(id = 100, stackable = false),
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
    fun `fixed amount banks inventory item when slot is valid`() {
        val client = TrackingClient().apply { IsBanking = true }

        PacketBankingService.handleFixedAmount(
            client = client,
            interfaceId = 5064,
            removeId = 100,
            removeSlot = 2,
            bankSlot = -1,
            amount = 5,
        )

        assertEquals(listOf(Triple(100, 2, 5)), client.bankItemCalls)
        assertEquals(1, client.itemUpdateCalls)
    }

    @Test
    fun `fixed amount fails closed when inventory slot is invalid`() {
        val client = TrackingClient().apply { IsBanking = true }

        assertDoesNotThrow {
            PacketBankingService.handleFixedAmount(
                client = client,
                interfaceId = 5064,
                removeId = 100,
                removeSlot = -1,
                bankSlot = -1,
                amount = 5,
            )
        }

        assertTrue(client.bankItemCalls.isEmpty())
        assertEquals(0, client.itemUpdateCalls)
    }

    @Test
    fun `bank all delegates to from bank for a valid bank slot`() {
        val client = TrackingClient()

        PacketBankingService.handleBankAll(
            client = client,
            interfaceId = 5382,
            removeSlot = 0,
            removeId = 100,
            bankSlot = 4,
            resolvedItemId = 100,
        )

        assertEquals(listOf(Triple(100, 4, -2)), client.fromBankCalls)
    }

    @Test
    fun `x amount ignores invalid inventory slot without mutating`() {
        val client =
            TrackingClient().apply {
                IsBanking = true
                XinterfaceID = 5064
                XremoveSlot = 999
                XremoveID = 100
            }

        assertDoesNotThrow {
            PacketBankingService.handleXAmount(client, 12)
        }

        assertTrue(client.bankItemCalls.isEmpty())
        assertEquals(0, client.itemUpdateCalls)
        assertEquals(-1, client.XinterfaceID)
        assertEquals(0, client.enterAmountId)
    }

    @Test
    fun `remove item from equipment rejects mismatched packet item id`() {
        val client =
            TrackingClient().apply {
                getEquipment()[3] = 4151
                getEquipmentN()[3] = 1
            }

        PacketBankingService.handleRemoveItem(
            client = client,
            interfaceId = 1688,
            removeSlot = 3,
            removeId = 4152,
            bankSlot = -1,
        )

        assertTrue(client.removeCalls.isEmpty())
        assertTrue(client.addItemCalls.isEmpty())
        assertEquals(4151, client.getEquipment()[3])
        assertEquals(1, client.getEquipmentN()[3])
    }

    private class TrackingClient : Client(EmbeddedChannel(), 1) {
        val bankItemCalls = mutableListOf<Triple<Int, Int, Int>>()
        val fromBankCalls = mutableListOf<Triple<Int, Int, Int>>()
        val removeCalls = mutableListOf<Pair<Int, Boolean>>()
        val addItemCalls = mutableListOf<Pair<Int, Int>>()
        var itemUpdateCalls = 0

        override fun bankItem(itemID: Int, fromSlot: Int, amount: Int) {
            bankItemCalls += Triple(itemID, fromSlot, amount)
        }

        override fun fromBank(itemID: Int, fromSlot: Int, amount: Int) {
            fromBankCalls += Triple(itemID, fromSlot, amount)
        }

        override fun checkItemUpdate() {
            itemUpdateCalls++
        }

        override fun remove(slot: Int, force: Boolean): Boolean {
            removeCalls += slot to force
            return true
        }

        override fun addItem(item: Int, amount: Int): Boolean {
            addItemCalls += item to amount
            return true
        }
    }

    companion object {
        private fun itemDefinition(id: Int, stackable: Boolean): Item =
            Item(
                id = id,
                slot = 3,
                standAnim = 808,
                walkAnim = 819,
                runAnim = 824,
                attackAnim = 806,
                shopSellValue = 1,
                shopBuyValue = 1,
                bonuses = IntArray(12),
                stackable = stackable,
                noteable = false,
                tradeable = true,
                twoHanded = false,
                full = false,
                mask = false,
                premium = false,
                name = "Test Item",
                description = "Test Item",
                alchemy = 0,
            )
    }
}
