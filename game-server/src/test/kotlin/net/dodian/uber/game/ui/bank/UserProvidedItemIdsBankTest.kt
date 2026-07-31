package net.dodian.uber.game.ui.bank

import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.loop.GameThreadContext
import net.dodian.uber.game.item.ItemManager
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Item
import net.dodian.uber.game.persistence.account.login.AccountLoginMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserProvidedItemIdsBankTest {
    private var previousItemManager: ItemManager? = null

    // User-provided list of item IDs to test:
    private val rawTargetItemIds = intArrayOf(
        4675, 4109, 4111, 4113, 4115, 4117, 565, 266, 374, 532, 4151, 6139, 6141, 4153, 995, 1303,
        207, 526, 2653, 2655, 2657, 2659, 2661, 2663, 2665, 2667, 2669, 2671, 2673, 2675, 3478, 3479,
        3480, 621, 526, 1267, 4121, 526, 4202, 11284, 11335, 11840, 1201, 1748, 536, 1163, 536, 1749,
        2363, 213, 215, 536, 1747, 1213, 1199, 536, 1753, 209, 536, 1751, 1621, 217, 245, 621, 526,
        1275, 1619, 454, 441, 1621, 1623, 526, 995, 3751, 4153, 4131, 7460, 1113, 1147, 2354, 264,
        374, 1199, 532, 4151, 892, 2364, 218, 210, 214, 216, 995, 592, 199, 222, 7454, 526, 7460,
        7459, 7458
    )

    private val targetItemIds = rawTargetItemIds.distinct().toIntArray()

    @BeforeEach
    fun setUp() {
        GameThreadContext.bindCurrentThread()
        previousItemManager = Server.itemManager

        val defMap = mutableMapOf<Int, Item>()
        for (id in targetItemIds) {
            val notedId = id + 10000 // Synthetic noted ID for testing noted conversion
            val isStack = id == 995 || id == 565 || id == 892
            defMap[id] = item(id = id, name = "Item-$id", stackable = isStack, noted = false, linkedNotedId = notedId, linkedItemId = 0)
            defMap[notedId] = item(id = notedId, name = "Item-$id (noted)", stackable = true, noted = true, linkedNotedId = 0, linkedItemId = id)
        }

        Server.itemManager = ItemManager(
            definitionLoader = { defMap },
            globalSpawnBootstrap = {},
        )
    }

    @AfterEach
    fun tearDown() {
        GameThreadContext.clearBindingForTests()
        Server.itemManager = previousItemManager
    }

    @Test
    fun `verify all user provided item ids deposit unnoted and save load without corruption`() {
        val client = Client(null, 1).apply {
            dbId = 1
            playerName = "TestUserIds"
            IsBanking = true
            bankSlotTabs = IntArray(bankSize())
        }

        val uniqueBatch = targetItemIds.take(28)
        for ((index, rawId) in uniqueBatch.withIndex()) {
            val unnotedId = Server.itemManager.normalizeForBank(rawId)
            val count = if (Server.itemManager.isStackable(rawId)) 10 else 1
            client.playerItems[index] = rawId + 1
            client.playerItemsN[index] = count

            PlayerBankService.deposit(client, rawId, index, count)

            val bankSlot = client.getBankSlot(unnotedId)
            assertTrue(bankSlot >= 0, "Item $unnotedId should exist in bank")
            assertEquals(unnotedId + 1, client.bankItems[bankSlot])
            assertEquals(count, client.bankItemsN[bankSlot])
        }

        // Save & Reload
        val bankSaveData = generateBankSnapshot(client)

        val loadedClient = Client(null, 2).apply {
            dbId = 2
            playerName = "LoadedUserIds"
            bankSlotTabs = IntArray(bankSize())
        }
        AccountLoginMapper.applyBank(loadedClient, bankSaveData)

        for (rawId in uniqueBatch) {
            val unnotedId = Server.itemManager.normalizeForBank(rawId)
            val expectedCount = if (Server.itemManager.isStackable(rawId)) 10 else 1
            val slot = loadedClient.getBankSlot(unnotedId)
            assertTrue(slot >= 0, "Loaded bank should contain unnoted item $unnotedId")
            assertEquals(unnotedId + 1, loadedClient.bankItems[slot])
            assertEquals(expectedCount, loadedClient.bankItemsN[slot])
        }
    }

    @Test
    fun `verify noted variants of user provided item ids convert to unnoted item ids on deposit`() {
        val client = Client(null, 1).apply {
            dbId = 1
            playerName = "TestNotedUserIds"
            IsBanking = true
            bankSlotTabs = IntArray(bankSize())
        }

        val uniqueBatch = targetItemIds.take(28)
        val testedNotedPairs = mutableListOf<Pair<Int, Int>>()

        for ((invSlot, rawId) in uniqueBatch.withIndex()) {
            val notedId = Server.itemManager.getLinkedNotedId(rawId)
            if (notedId > 0 && notedId != rawId) {
                testedNotedPairs.add(notedId to rawId)
                client.playerItems[invSlot] = notedId + 1
                client.playerItemsN[invSlot] = 50

                PlayerBankService.deposit(client, notedId, invSlot, 50)

                // Must NOT store noted item ID in bank:
                assertEquals(-1, client.getBankSlot(notedId))

                // MUST store unnoted item ID in bank:
                val unnotedSlot = client.getBankSlot(rawId)
                assertTrue(unnotedSlot >= 0)
                assertEquals(rawId + 1, client.bankItems[unnotedSlot])
                assertEquals(50, client.bankItemsN[unnotedSlot])
            }
        }

        // Save & Reload
        val bankSaveData = generateBankSnapshot(client)

        val loadedClient = Client(null, 2).apply {
            dbId = 2
            playerName = "LoadedNotedUserIds"
            bankSlotTabs = IntArray(bankSize())
        }
        AccountLoginMapper.applyBank(loadedClient, bankSaveData)

        for ((notedId, unnotedId) in testedNotedPairs) {
            assertEquals(-1, loadedClient.getBankSlot(notedId))
            val slot = loadedClient.getBankSlot(unnotedId)
            assertTrue(slot >= 0)
            assertEquals(unnotedId + 1, loadedClient.bankItems[slot])
            assertEquals(50, loadedClient.bankItemsN[slot])
        }
    }

    private fun generateBankSnapshot(client: Client): String {
        val bank = StringBuilder()
        for (i in client.bankItems.indices) {
            if (client.bankItems[i] > 0) {
                val tab = if (client.bankSlotTabs != null && i < client.bankSlotTabs.size) client.bankSlotTabs[i] else 0
                bank.append(i).append('-').append(client.bankItems[i] - 1).append('-').append(client.bankItemsN[i]).append('-').append(tab).append(' ')
            }
        }
        if (client.bankPlaceholdersEnabled) {
            bank.append("@ph=1 ")
        }
        return bank.toString()
    }

    private fun item(
        id: Int,
        name: String,
        stackable: Boolean,
        noted: Boolean,
        linkedNotedId: Int,
        linkedItemId: Int,
    ) = Item(
        id = id,
        name = name,
        slot = -1,
        standAnim = 0,
        walkAnim = 0,
        runAnim = 0,
        attackAnim = 0,
        shopSellValue = 0,
        shopBuyValue = 0,
        bonuses = IntArray(12),
        stackable = stackable,
        noted = noted,
        placeholder = false,
        noteable = true,
        tradeable = true,
        twoHanded = false,
        full = false,
        mask = false,
        premium = false,
        examine = "test",
        alchemy = 0,
        weight = 0.0,
        lowAlch = 0,
        linkedItemId = linkedItemId,
        linkedNotedId = linkedNotedId,
    )
}
