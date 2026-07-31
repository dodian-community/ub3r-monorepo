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

class PlayerBankSaveLoadVerificationTest {
    private var previousItemManager: ItemManager? = null

    // Definitions setup:
    // Item 4151: Abyssal Whip (unnoted, non-stackable)
    // Item 385:  Shark (unnoted, non-stackable, linkedNotedId = 386)
    // Item 386:  Noted Shark (noted, stackable, linkedItemId = 385)
    // Item 995:  Coins (unnoted, stackable)
    @BeforeEach
    fun setUp() {
        GameThreadContext.bindCurrentThread()
        previousItemManager = Server.itemManager
        Server.itemManager = ItemManager(
            definitionLoader = {
                mapOf(
                    4151 to item(4151, name = "Abyssal whip", stackable = false, noted = false, linkedNotedId = 0, linkedItemId = 0),
                    385 to item(385, name = "Shark", stackable = false, noted = false, linkedNotedId = 386, linkedItemId = 0),
                    386 to item(386, name = "Shark (noted)", stackable = true, noted = true, linkedNotedId = 0, linkedItemId = 385),
                    995 to item(995, name = "Coins", stackable = true, noted = false, linkedNotedId = 0, linkedItemId = 0),
                )
            },
            globalSpawnBootstrap = {},
        )
    }

    @AfterEach
    fun tearDown() {
        GameThreadContext.clearBindingForTests()
        Server.itemManager = previousItemManager
    }

    @Test
    fun `depositing unnoted item keeps exact item id in bank`() {
        val client = Client(null, 1).apply {
            IsBanking = true
            playerItems[0] = 4152 // Whip (id 4151 + 1)
            playerItemsN[0] = 1
        }

        PlayerBankService.deposit(client, 4151, 0, 1)

        val bankSlot = client.getBankSlot(4151)
        assertTrue(bankSlot >= 0, "Unnoted Whip should exist in bank")
        assertEquals(4152, client.bankItems[bankSlot], "Bank item ID (+1) must match raw ID + 1")
        assertEquals(1, client.bankItemsN[bankSlot], "Bank amount should be 1")
        assertEquals(0, client.playerItems[0], "Inventory slot should be cleared")
    }

    @Test
    fun `depositing noted item converts to unnoted item id in bank`() {
        val client = Client(null, 1).apply {
            IsBanking = true
            playerItems[0] = 387 // Noted Shark (id 386 + 1)
            playerItemsN[0] = 25
        }

        PlayerBankService.deposit(client, 386, 0, 25)

        // Should NOT store item 386 in bank:
        val notedSlot = client.getBankSlot(386)
        assertEquals(-1, notedSlot, "Noted item ID (386) must NOT exist in bank")

        // Should store unnoted item 385 in bank:
        val unnotedSlot = client.getBankSlot(385)
        assertTrue(unnotedSlot >= 0, "Unnoted item ID (385) MUST be stored in bank")
        assertEquals(386, client.bankItems[unnotedSlot], "Bank item ID (+1) must be 385 + 1 = 386")
        assertEquals(25, client.bankItemsN[unnotedSlot], "Bank amount should be 25")
    }

    @Test
    fun `depositing noted item merges into existing unnoted bank stack`() {
        val client = Client(null, 1).apply {
            IsBanking = true
            // Pre-existing 10 unnoted Sharks in bank slot 2:
            bankItems[2] = 386 // ID 385 + 1
            bankItemsN[2] = 10

            // Noted Shark in inventory slot 0:
            playerItems[0] = 387 // ID 386 + 1
            playerItemsN[0] = 15
        }

        PlayerBankService.deposit(client, 386, 0, 15)

        assertEquals(386, client.bankItems[2], "Item ID must remain 385 (+1)")
        assertEquals(25, client.bankItemsN[2], "Amount in bank must be 10 + 15 = 25")
    }

    @Test
    fun `bank save and load roundtrip preserves item ids and quantities exactly`() {
        val originalClient = Client(null, 1).apply {
            dbId = 1
            playerName = "TestPlayer"
            bankSlotTabs = IntArray(bankSize())
            // Setup bank items:
            bankItems[0] = 4152 // Whip (id 4151)
            bankItemsN[0] = 2
            bankSlotTabs[0] = 1

            bankItems[1] = 386 // Unnoted Shark (id 385)
            bankItemsN[1] = 500
            bankSlotTabs[1] = 1

            bankItems[2] = 996 // Coins (id 995)
            bankItemsN[2] = 1_000_000
            bankSlotTabs[2] = 2
        }

        val bankSaveData = generateBankSnapshot(originalClient)

        // Load into new client:
        val loadedClient = Client(null, 2).apply {
            dbId = 2
            playerName = "TestPlayer2"
            bankSlotTabs = IntArray(bankSize())
        }
        AccountLoginMapper.applyBank(loadedClient, bankSaveData)

        // Verify loaded client matches original client 1:1:
        assertEquals(4152, loadedClient.bankItems[0], "Slot 0 ID must match Whip")
        assertEquals(2, loadedClient.bankItemsN[0], "Slot 0 amount must be 2")
        assertEquals(1, loadedClient.bankSlotTabs[0], "Slot 0 tab must be 1")

        assertEquals(386, loadedClient.bankItems[1], "Slot 1 ID must match Unnoted Shark")
        assertEquals(500, loadedClient.bankItemsN[1], "Slot 1 amount must be 500")
        assertEquals(1, loadedClient.bankSlotTabs[1], "Slot 1 tab must be 1")

        assertEquals(996, loadedClient.bankItems[2], "Slot 2 ID must match Coins")
        assertEquals(1_000_000, loadedClient.bankItemsN[2], "Slot 2 amount must be 1,000,000")
        assertEquals(2, loadedClient.bankSlotTabs[2], "Slot 2 tab must be 2")
    }

    @Test
    fun `placeholder item in bank persists across save and load roundtrip`() {
        val originalClient = Client(null, 1).apply {
            dbId = 1
            playerName = "TestPlayer"
            bankSlotTabs = IntArray(bankSize())
            bankPlaceholdersEnabled = true
            bankItems[0] = 4152 // Whip
            bankItemsN[0] = 0   // Placeholder (0 count)
            bankSlotTabs[0] = 3
        }

        val bankSaveData = generateBankSnapshot(originalClient)

        val loadedClient = Client(null, 2).apply {
            dbId = 2
            playerName = "TestPlayer2"
            bankSlotTabs = IntArray(bankSize())
        }
        AccountLoginMapper.applyBank(loadedClient, bankSaveData)

        assertEquals(4152, loadedClient.bankItems[0], "Placeholder Whip ID must persist")
        assertEquals(0, loadedClient.bankItemsN[0], "Placeholder amount must be 0")
        assertEquals(3, loadedClient.bankSlotTabs[0], "Placeholder tab must persist")
        assertTrue(loadedClient.bankPlaceholdersEnabled, "Placeholders enabled flag must persist")
    }

    @Test
    fun `tarnish json definition maps noted and unnoted links correctly`() {
        val baseDef = net.dodian.uber.game.item.ItemDefBase(
            id = 7,
            name = "Cannon base",
            unnotedId = 6,
            stackable = true,
        )
        val jsonDef = net.dodian.uber.game.item.ItemDefJson(
            id = 7,
            name = "Cannon base",
            unnotedId = 6,
            stackable = true,
        )

        val item7 = Item.fromDefs(baseDef, jsonDef)

        assertTrue(item7.isNoted(), "Item 7 must be recognized as noted because unnotedId is 6")
        assertEquals(6, item7.linkedItemId, "Item 7 linkedItemId must be 6")
        assertTrue(item7.getStackable(), "Noted item must be stackable")
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
