package net.dodian.uber.game.engine.systems.inventory

import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.loop.GameThreadContext
import net.dodian.uber.game.item.ItemManager
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Item
import net.dodian.uber.game.model.item.transaction.BankTransactions
import net.dodian.uber.game.model.item.transaction.OfferTransactions
import net.dodian.uber.game.persistence.player.PlayerSaveSegment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

class ClientInventoryTransactionTest {
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
                    300 to item(300, stackable = true),
                    995 to item(995, stackable = true),
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
    fun `failed transaction leaves live inventory and dirty mask unchanged`() {
        val client = Client(null, 1).apply {
            playerItems[0] = 101
            playerItemsN[0] = 1
        }

        val committed = client.inventoryTransaction {
            remove(100, 1)
            remove(200, 1)
        }

        assertFalse(committed)
        assertEquals(101, client.playerItems[0])
        assertEquals(1, client.playerItemsN[0])
        assertEquals(0, client.saveDirtyMask)
    }

    @Test
    fun `successful replacement commits once and marks inventory dirty`() {
        val client = Client(null, 1).apply {
            playerItems[0] = 101
            playerItemsN[0] = 1
        }

        val committed = client.inventoryTransaction {
            remove(100, 1)
            add(200, 1)
        }

        assertTrue(committed)
        assertEquals(201, client.playerItems[0])
        assertEquals(1, client.playerItemsN[0])
        assertTrue(client.saveDirtyMask and PlayerSaveSegment.INVENTORY.mask != 0)
    }

    @Test
    fun `slot-bound removal rejects a stale item slot`() {
        val client = Client(null, 1).apply {
            playerItems[0] = 101
            playerItemsN[0] = 1
        }

        assertFalse(client.inventoryTransaction { removeAt(0, 200, 1) })
        assertEquals(101, client.playerItems[0])
    }

    @Test
    fun `bank transfer commits inventory and bank together`() {
        val client = Client(null, 1).apply {
            bankItems[4] = 101
            bankItemsN[4] = 3
        }

        assertTrue(BankTransactions.withdraw(client, 100, 4, 2))

        assertEquals(101, client.playerItems[0])
        assertEquals(1, client.playerItemsN[0])
        assertEquals(101, client.playerItems[1])
        assertEquals(1, client.playerItemsN[1])
        assertEquals(101, client.bankItems[4])
        assertEquals(1, client.bankItemsN[4])
        assertTrue(client.saveDirtyMask and PlayerSaveSegment.INVENTORY.mask != 0)
        assertTrue(client.saveDirtyMask and PlayerSaveSegment.BANK.mask != 0)
    }

    @Test
    fun `complete bank withdrawal retains zero amount item when placeholder requested`() {
        val client = Client(null, 1).apply {
            bankItems[4] = 101
            bankItemsN[4] = 1
            bankSlotTabs = IntArray(bankSize()).also { it[4] = 2 }
        }

        assertTrue(BankTransactions.withdraw(client, 100, 4, 1, retainPlaceholder = true))

        assertEquals(101, client.bankItems[4])
        assertEquals(0, client.bankItemsN[4])
        assertEquals(2, client.bankSlotTabs[4])
    }

    @Test
    fun `complete bank withdrawal clears slot without placeholder request`() {
        val client = Client(null, 1).apply {
            bankItems[4] = 101
            bankItemsN[4] = 1
        }

        assertTrue(BankTransactions.withdraw(client, 100, 4, 1))

        assertEquals(0, client.bankItems[4])
        assertEquals(0, client.bankItemsN[4])
    }

    @Test
    fun `sequential complete withdrawals expose each placeholder to its first refresh`() {
        val snapshots = mutableListOf<List<Pair<Int, Int>>>()
        val client = object : Client(null, 1) {
            override fun checkItemUpdate() {
                snapshots += bankItems.indices
                    .filter { bankItems[it] > 0 }
                    .map { bankItems[it] to bankItemsN[it] }
            }
        }.apply {
            bankItems[3] = 101
            bankItemsN[3] = 1
            bankItems[4] = 201
            bankItemsN[4] = 1
        }

        assertTrue(BankTransactions.withdraw(client, 100, 3, 1, retainPlaceholder = true))
        assertEquals(listOf(101 to 0, 201 to 1), snapshots.single())

        assertTrue(BankTransactions.withdraw(client, 200, 4, 1, retainPlaceholder = true))
        assertEquals(listOf(101 to 0, 201 to 0), snapshots.last())
        assertEquals(2, snapshots.size)
    }

    @Test
    fun `failed retained withdrawal leaves bank entry unchanged`() {
        val client = Client(null, 1).apply {
            bankItems[4] = 101
            bankItemsN[4] = 2
            playerItems.indices.forEach { slot ->
                playerItems[slot] = 201
                playerItemsN[slot] = 1
            }
        }

        assertFalse(BankTransactions.withdraw(client, 100, 4, 2, retainPlaceholder = true))

        assertEquals(101, client.bankItems[4])
        assertEquals(2, client.bankItemsN[4])
    }

    @Test
    fun `inventory deposit adds exactly the requested non stackable amount`() {
        val client = Client(null, 1).apply {
            repeat(3) { slot ->
                playerItems[slot] = 101
                playerItemsN[slot] = 1
            }
        }

        assertTrue(BankTransactions.deposit(client, 100, 0, 2))

        assertEquals(1, client.playerItems.count { it == 101 })
        assertEquals(2, client.bankItemsN.single { it > 0 })
    }

    @Test
    fun `inventory deposit adds exactly the requested stackable amount`() {
        val client = Client(null, 1).apply {
            playerItems[0] = 996
            playerItemsN[0] = 100
        }

        assertTrue(BankTransactions.deposit(client, 995, 0, 40))

        assertEquals(60, client.playerItemsN[0])
        assertEquals(40, client.bankItemsN.single { it > 0 })
    }

    @Test
    fun `stale inventory deposit leaves inventory and bank untouched`() {
        val client = Client(null, 1).apply {
            playerItems[0] = 101
            playerItemsN[0] = 1
        }

        assertFalse(BankTransactions.deposit(client, 100, 1, 1))

        assertEquals(101, client.playerItems[0])
        assertTrue(client.bankItems.all { it == 0 })
    }

    @Test
    fun `noted inventory deposit stores the declared unnoted bank item`() {
        val client = Client(null, 1).apply {
            playerItems[0] = 301
            playerItemsN[0] = 12
        }

        assertTrue(BankTransactions.deposit(client, 300, 0, 7, bankItemId = 100))

        assertEquals(5, client.playerItemsN[0])
        val bankSlot = client.bankItems.indexOf(101)
        assertTrue(bankSlot >= 0)
        assertEquals(7, client.bankItemsN[bankSlot])
        assertFalse(client.bankItems.contains(301))
    }

    @Test
    fun `full bank rejects a new entry without removing inventory`() {
        val client = Client(null, 1).apply {
            playerItems[0] = 101
            playerItemsN[0] = 1
            bankItems.indices.forEach { slot ->
                bankItems[slot] = 10_000 + slot
                bankItemsN[slot] = 1
            }
        }

        assertFalse(BankTransactions.deposit(client, 100, 0, 1))

        assertEquals(101, client.playerItems[0])
        assertEquals(1, client.playerItemsN[0])
        assertFalse(client.bankItems.contains(101))
    }

    @Test
    fun `bank stack overflow rolls deposit back`() {
        val client = Client(null, 1).apply {
            playerItems[0] = 996
            playerItemsN[0] = 1
            bankItems[0] = 996
            bankItemsN[0] = maxItemAmount
        }

        assertFalse(BankTransactions.deposit(client, 995, 0, 1))

        assertEquals(1, client.playerItemsN[0])
        assertEquals(client.maxItemAmount, client.bankItemsN[0])
    }

    @Test
    fun `failed paired trade projection leaves both inventories and reservations intact`() {
        val first = Client(null, 1).apply {
            dbId = 10
            playerItems[0] = 101
            playerItemsN[0] = 1
        }
        val second = Client(null, 2).apply {
            dbId = 20
            playerItems.indices.forEach { slot ->
                playerItems[slot] = 200 + slot
                playerItemsN[slot] = 1
            }
        }
        net.dodian.uber.game.social.exchange.ExchangeRuntime.create(
            net.dodian.uber.game.api.plugin.social.ExchangeKind.TRADE, first, second,
        )
        assertTrue(net.dodian.uber.game.social.exchange.ExchangeRuntime.reserve(first, 0, 100, 1))

        assertNull(OfferTransactions.projectReservedTrade(first, second))

        assertEquals(1, net.dodian.uber.game.social.exchange.ExchangeRuntime.offers(first).size)
        assertEquals(100, net.dodian.uber.game.social.exchange.ExchangeRuntime.offers(first).single().id)
        assertTrue(net.dodian.uber.game.social.exchange.ExchangeRuntime.offers(second).isEmpty())
        assertEquals(200, second.playerItems[0])
        assertEquals(0, first.saveDirtyMask)
        assertEquals(0, second.saveDirtyMask)
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
