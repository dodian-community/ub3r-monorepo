package net.dodian.uber.game.content.objects

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ObjectContentDispatcherTest {

    private lateinit var client: Client
    private val pos = Position(3200, 3200, 0)

    @BeforeEach
    fun setUp() {
        ObjectContentRegistry.clearForTests()
        client = Client(null, 1)
    }

    @AfterEach
    fun tearDown() {
        ObjectContentRegistry.resetForTests()
    }

    @Test
    fun `click routes correctly`() {
        val content = object : ObjectContent {
            override val objectIds: IntArray = intArrayOf(1000)
            override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: net.dodian.cache.`object`.GameObjectData?) = true
            override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: net.dodian.cache.`object`.GameObjectData?) = true
            override fun onThirdClick(client: Client, objectId: Int, position: Position, obj: net.dodian.cache.`object`.GameObjectData?) = true
        }
        ObjectContentRegistry.register("RouteClicks", content)

        assertTrue(ObjectContentDispatcher.tryHandleClick(client, 1, 1000, pos, null))
        assertTrue(ObjectContentDispatcher.tryHandleClick(client, 2, 1000, pos, null))
        assertTrue(ObjectContentDispatcher.tryHandleClick(client, 3, 1000, pos, null))
        assertFalse(ObjectContentDispatcher.tryHandleClick(client, 4, 1000, pos, null))
    }

    @Test
    fun `item and magic route correctly`() {
        val content = object : ObjectContent {
            override val objectIds: IntArray = intArrayOf(1001)
            override fun onUseItem(
                client: Client,
                objectId: Int,
                position: Position,
                obj: net.dodian.cache.`object`.GameObjectData?,
                itemId: Int,
                itemSlot: Int,
                interfaceId: Int,
            ) = itemId == 2357 && itemSlot == 3 && interfaceId == 3214

            override fun onMagic(
                client: Client,
                objectId: Int,
                position: Position,
                obj: net.dodian.cache.`object`.GameObjectData?,
                spellId: Int,
            ) = spellId == 1186
        }
        ObjectContentRegistry.register("RouteItemMagic", content)

        assertTrue(ObjectContentDispatcher.tryHandleUseItem(client, 1001, pos, null, 2357, 3, 3214))
        assertTrue(ObjectContentDispatcher.tryHandleMagic(client, 1001, pos, null, 1186))
        assertFalse(ObjectContentDispatcher.tryHandleUseItem(client, 1001, pos, null, 2351, 3, 3214))
        assertFalse(ObjectContentDispatcher.tryHandleMagic(client, 1001, pos, null, 1182))
    }

    @Test
    fun `exceptions return false`() {
        val content = object : ObjectContent {
            override val objectIds: IntArray = intArrayOf(1002)
            override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: net.dodian.cache.`object`.GameObjectData?): Boolean {
                error("boom")
            }
        }
        ObjectContentRegistry.register("Throwing", content)

        assertFalse(ObjectContentDispatcher.tryHandleClick(client, 1, 1002, pos, null))
    }

    @Test
    fun `binding resolution uses exact then range then id fallback`() {
        val calls = mutableListOf<String>()

        val fallback = object : ObjectContent {
            override fun bindings(): List<ObjectBinding> = listOf(ObjectBinding(objectId = 2000))
            override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: net.dodian.cache.`object`.GameObjectData?): Boolean {
                calls += "fallback"
                return true
            }
        }
        val range = object : ObjectContent {
            override fun bindings(): List<ObjectBinding> = listOf(
                ObjectBinding.inRange(objectId = 2000, minX = 3190, maxX = 3210, minY = 3190, maxY = 3210, plane = 0),
            )

            override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: net.dodian.cache.`object`.GameObjectData?): Boolean {
                calls += "range"
                return true
            }
        }
        val exact = object : ObjectContent {
            override fun bindings(): List<ObjectBinding> = listOf(ObjectBinding.at(objectId = 2000, x = 3200, y = 3200, z = 0))
            override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: net.dodian.cache.`object`.GameObjectData?): Boolean {
                calls += "exact"
                return true
            }
        }

        ObjectContentRegistry.register("Fallback", fallback)
        ObjectContentRegistry.register("Range", range)
        ObjectContentRegistry.register("Exact", exact)

        assertTrue(ObjectContentDispatcher.tryHandleClick(client, 1, 2000, Position(3200, 3200, 0), null))
        assertTrue(ObjectContentDispatcher.tryHandleClick(client, 1, 2000, Position(3199, 3199, 0), null))
        assertTrue(ObjectContentDispatcher.tryHandleClick(client, 1, 2000, Position(3300, 3300, 0), null))

        assertEquals(listOf("exact", "range", "fallback"), calls)
    }
}
