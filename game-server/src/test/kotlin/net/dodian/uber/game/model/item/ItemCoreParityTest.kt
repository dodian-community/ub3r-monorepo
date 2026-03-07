package net.dodian.uber.game.model.item

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.Server
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ItemCoreParityTest {
    @AfterEach
    fun tearDown() {
        Ground.ground_items.clear()
        Ground.untradeable_items.clear()
        Ground.tradeable_items.clear()
        Server.itemManager = null
    }

    @Test
    fun `equipment slot ids remain unchanged`() {
        assertEquals(14, Equipment.SIZE)
        assertEquals(0, Equipment.Slot.HEAD.id)
        assertEquals(1, Equipment.Slot.CAPE.id)
        assertEquals(2, Equipment.Slot.NECK.id)
        assertEquals(3, Equipment.Slot.WEAPON.id)
        assertEquals(4, Equipment.Slot.CHEST.id)
        assertEquals(5, Equipment.Slot.SHIELD.id)
        assertEquals(7, Equipment.Slot.LEGS.id)
        assertEquals(8, Equipment.Slot.BLESSING.id)
        assertEquals(9, Equipment.Slot.HANDS.id)
        assertEquals(10, Equipment.Slot.FEET.id)
        assertEquals(12, Equipment.Slot.RING.id)
        assertEquals(13, Equipment.Slot.ARROWS.id)
    }

    @Test
    fun `item container slot set and get remain unchanged`() {
        val container = TestItemContainer(2)
        val item = GameItem(42, 7)

        container.put(1, item)

        assertEquals(item, container.fetch(1))
    }

    @Test
    fun `game item stackable resolution and amount mutation remain unchanged`() {
        Server.itemManager = testItemManager(
            item(
                id = 995,
                slot = 3,
                stackable = true,
                tradable = true,
            ),
        )

        val item = GameItem(995, 10)
        item.addAmount(5)
        item.removeAmount(3)

        assertTrue(item.isStackable())
        assertEquals(12, item.getAmount())
    }

    @Test
    fun `ground pickup visibility and lookup precedence remain unchanged`() {
        Server.itemManager = testItemManager(item(id = 100, slot = 3, tradable = true))
        val client = Client(EmbeddedChannel(), 1).apply { dbId = 55 }

        val staticItem = GroundItem(Position(3200, 3200, 0), 100, 1, 50, true)
        val tradeableItem = GroundItem(Position(3200, 3200, 0), 100, 2, 50, true).apply {
            type = 2
            playerId = 99
        }
        val ownerOnlyItem = GroundItem(Position(3200, 3200, 0), 100, 3, 50, false).apply {
            type = 1
            playerId = 55
        }

        Ground.ground_items += staticItem
        Ground.tradeable_items += tradeableItem
        Ground.untradeable_items += ownerOnlyItem

        assertTrue(Ground.canPickup(client, staticItem))
        assertTrue(Ground.canPickup(client, ownerOnlyItem))
        assertTrue(Ground.isTracked(tradeableItem))
        assertEquals(staticItem, Ground.findGroundItem(client, 100, 3200, 3200, 0))
    }

    @Test
    fun `ground item show and despawn timing remain unchanged`() {
        Server.itemManager = testItemManager(item(id = 100, slot = 3, tradable = true))
        val item = GroundItem(Position(3200, 3200, 0), 100, 1, 10, false)

        assertEquals(10, item.getDespawnTime())
        item.reduceTime()
        assertEquals(9, item.getDespawnTime())

        val delayed = GroundItem(Position(3200, 3200, 0), 100, 1, 5, false)
        delayed.timeToShow = 2
        delayed.timeToDespawn = 5

        delayed.reduceTime()
        assertEquals(6, delayed.getDespawnTime())
    }

    @Test
    fun `item manager lookup fallbacks remain unchanged`() {
        val item = item(
            id = 4151,
            slot = 3,
            standAnim = 809,
            walkAnim = 820,
            runAnim = 825,
            attackAnim = 451,
            stackable = false,
            noteable = true,
            tradable = true,
            twoHanded = true,
            full = false,
            mask = false,
            premium = true,
            shopSellValue = 10,
            shopBuyValue = 20,
            alchemy = 30,
            bonuses = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12),
            name = "Abyssal whip",
            description = "A weapon",
        )
        val manager = testItemManager(item)

        assertTrue(manager.isNote(4151))
        assertTrue(manager.isTwoHanded(4151))
        assertTrue(manager.isPremium(4151))
        assertTrue(manager.isTradable(4151))
        assertEquals(3, manager.getSlot(4151))
        assertEquals(809, manager.getStandAnim(4151))
        assertEquals(820, manager.getWalkAnim(4151))
        assertEquals(825, manager.getRunAnim(4151))
        assertEquals(451, manager.getAttackAnim(4151))
        assertEquals(12, manager.getBonus(4151, 11))
        assertEquals(10, manager.getShopSellValue(4151))
        assertEquals(20, manager.getShopBuyValue(4151))
        assertEquals(30, manager.getAlchemy(4151))
        assertEquals("Abyssal whip", manager.getName(4151))
        assertEquals("A weapon", manager.getExamine(4151))
        assertEquals(3, manager.getSlot(-1))
        assertEquals(808, manager.getStandAnim(-1))
        assertFalse(manager.isTradable(4084))
        assertTrue(manager.items.containsKey(4151))
    }

    @Test
    fun `reload items replaces definitions from loader`() {
        val definitions = arrayOf(
            linkedMapOf(1 to item(id = 1, slot = 3, name = "One")),
            linkedMapOf(2 to item(id = 2, slot = 4, name = "Two")),
        )
        var index = 0
        val manager =
            ItemManager(
                definitionLoader = {
                    val current = definitions[index.coerceAtMost(definitions.lastIndex)]
                    index++
                    current
                },
                globalSpawnBootstrap = {},
            )

        assertNotNull(manager.items[1])
        assertNull(manager.items[2])

        manager.reloadItems()

        assertNull(manager.items[1])
        assertNotNull(manager.items[2])
    }

    @Test
    fun `global ground spawn content matches hardcoded snapshot`() {
        val spawns = net.dodian.uber.game.content.items.spawn.GlobalGroundSpawnContent.spawns

        assertEquals(48, spawns.size)
        assertTrue(spawns.any { it.position == Position(2611, 3096, 0) && it.itemId == 11862 && it.amount == 1 && it.displayTime == 100 })
        assertTrue(spawns.any { it.position == Position(2605, 3104, 0) && it.itemId == 1277 && it.displayTime == 33 })
        assertTrue(spawns.any { it.position == Position(3143, 2991, 0) && it.itemId == 401 && it.displayTime == 25 })
        assertTrue(spawns.any { it.position == Position(2642, 3240, 0) && it.itemId == 401 && it.displayTime == 25 })
    }

    private fun testItemManager(vararg definitions: Item): ItemManager {
        val byId = LinkedHashMap<Int, Item>()
        for (definition in definitions) {
            byId[definition.getId()] = definition
        }
        return ItemManager(
            definitionLoader = { byId },
            globalSpawnBootstrap = {},
        ).also { Server.itemManager = it }
    }

    private fun item(
        id: Int,
        slot: Int,
        standAnim: Int = 808,
        walkAnim: Int = 819,
        runAnim: Int = 824,
        attackAnim: Int = 806,
        shopSellValue: Int = 1,
        shopBuyValue: Int = 0,
        bonuses: IntArray = IntArray(12),
        stackable: Boolean = false,
        noteable: Boolean = false,
        tradable: Boolean = true,
        twoHanded: Boolean = false,
        full: Boolean = false,
        mask: Boolean = false,
        premium: Boolean = false,
        name: String = "Item $id",
        description: String = "Desc $id",
        alchemy: Int = 0,
    ): Item =
        Item(
            id = id,
            slot = slot,
            standAnim = standAnim,
            walkAnim = walkAnim,
            runAnim = runAnim,
            attackAnim = attackAnim,
            shopSellValue = shopSellValue,
            shopBuyValue = shopBuyValue,
            bonuses = bonuses,
            stackable = stackable,
            noteable = noteable,
            tradeable = tradable,
            twoHanded = twoHanded,
            full = full,
            mask = mask,
            premium = premium,
            name = name,
            description = description,
            alchemy = alchemy,
        )

    private class TestItemContainer(size: Int) : ItemContainer(size) {
        fun fetch(slot: Int): GameItem = getSlot(slot)

        fun put(slot: Int, item: GameItem) {
            setSlot(slot, item)
        }
    }
}
