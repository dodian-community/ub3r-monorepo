package net.dodian.uber.game.engine.systems.inventory

import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.loop.GameThreadContext
import net.dodian.uber.game.item.ItemManager
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Item
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EquipmentServiceTest {
    private var previousItemManager: ItemManager? = null

    @BeforeEach
    fun setUp() {
        GameThreadContext.bindCurrentThread()
        previousItemManager = Server.itemManager
        Server.itemManager = ItemManager(
            definitionLoader = { mapOf(100 to item(100), 200 to item(200)) },
            globalSpawnBootstrap = {},
        )
    }

    @AfterEach
    fun tearDown() {
        GameThreadContext.clearBindingForTests()
        Server.itemManager = previousItemManager
    }

    @Test
    fun `wearing into an empty slot does not insert the zero amount equipment sentinel`() {
        val client = Client(null, 1).apply {
            playerItems[0] = 101
            playerItemsN[0] = 1
        }

        EquipmentService.wear(client, 100, 0, 3214)

        assertEquals(100, client.equipment[3])
        assertEquals(1, client.equipmentN[3])
        assertEquals(0, client.playerItems[0])
    }

    @Test
    fun `wear swap succeeds when inventory was full because selected slot becomes available`() {
        val client = Client(null, 1).apply {
            playerItems.indices.forEach { slot ->
                playerItems[slot] = if (slot == 0) 101 else 201
                playerItemsN[slot] = 1
            }
            equipment[3] = 200
            equipmentN[3] = 1
        }

        EquipmentService.wear(client, 100, 0, 3214)

        assertEquals(100, client.equipment[3])
        assertEquals(201, client.playerItems[0])
        assertEquals(1, client.playerItemsN[0])
        assertTrue(client.playerItems.drop(1).all { it == 201 })
    }

    private fun item(id: Int) = Item(
        id = id, name = "test-$id", slot = 3,
        standAnim = 0, walkAnim = 0, runAnim = 0, attackAnim = 0,
        shopSellValue = 0, shopBuyValue = 0, bonuses = IntArray(12),
        stackable = false, noteable = false, tradeable = true, twoHanded = false,
        full = false, mask = false, premium = false, examine = "test", alchemy = 0,
    )
}
