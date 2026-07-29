package net.dodian.uber.game.stability

import net.dodian.cache.objects.GameObjectData
import net.dodian.uber.game.combat.TomlProjectileLoader
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.engine.systems.interaction.ui.TradeDuelSessionService
import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Item
import net.dodian.uber.game.item.ItemManager
import net.dodian.uber.game.ui.PartyRoomInterface
import net.dodian.uber.game.ui.buttons.InterfaceButtonRegistry
import net.dodian.uber.game.netty.listener.out.Projectile
import net.dodian.uber.game.social.exchange.TradingService
import net.dodian.uber.game.social.exchange.ExchangeRuntime
import java.nio.file.Files
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StabilityHardeningTest {
    @BeforeEach
    fun setUpItems() {
        Server.itemManager = ItemManager({
            mapOf(
                995 to item(995, true),
                100 to item(100, false),
            )
        }, {})
    }

    @AfterEach
    fun clearRegisteredTestPlayers() {
        PlayerRegistry.players[1] = null
        PlayerRegistry.players[2] = null
    }

    @Test
    fun `player registry rejects every out of range player slot`() {
        val invalidSlots = intArrayOf(-1, PlayerRegistry.players.size, 65535)

        invalidSlots.forEach { slot ->
            assertNull(PlayerRegistry.getClient(slot))
            assertFalse(PlayerRegistry.validClient(slot))
        }
    }

    @Test
    fun `unknown object lookups do not grow the definition cache`() {
        val before = GameObjectData.definitionCount()

        val first = GameObjectData.forId(999_991)
        val second = GameObjectData.forId(999_991)

        assertEquals(999_991, first.id)
        assertEquals(999_991, second.id)
        assertEquals(before, GameObjectData.definitionCount())
    }

    @Test
    fun `party room accept is bound only to current client interface component`() {
        val binding = PartyRoomInterface.bindings.single()
        assertEquals(2156, binding.interfaceId)
        assertEquals(2246, binding.componentId)
        assertTrue(2246 in binding.rawButtonIds)
        assertFalse(8198 in binding.rawButtonIds)
        assertEquals(2156, binding.requiredInterfaceId)

        val client = Client(null, 1)
        client.activeInterfaceId = 2156
        assertEquals("partyroom.deposit.accept", InterfaceButtonRegistry.resolve(client, 2246, -1)?.componentKey)

        client.activeInterfaceId = 3214
        assertNull(InterfaceButtonRegistry.resolve(client, 2246, -1))
        assertNull(InterfaceButtonRegistry.resolve(client, 8198, -1))
    }

    @Test
    fun `damage attribution can be explicitly released at lifecycle boundaries`() {
        val owner = Client(null, 1)
        val attacker = Client(null, 2)
        owner.damage[attacker] = 12

        owner.clearDamageAttribution()

        assertTrue(owner.damage.isEmpty())
    }

    @Test
    fun `projectile configuration retains the serialized slope and rejects dead angle state`() {
        val file = Files.createTempFile("projectiles", ".toml")
        Files.writeString(
            file,
            """
            [[projectile]]
            name = "test_projectile"
            id = 42
            startHeight = 12
            endHeight = 20
            delay = 33
            slope = 7
            """.trimIndent(),
        )

        val entry = TomlProjectileLoader.load(file.toString()).single()

        assertEquals(7, entry.def.slope)
        assertFalse(Projectile::class.java.declaredFields.any { it.name == "angle" })
        Files.deleteIfExists(file)
    }

    @Test
    fun `trade cancellation refunds both inventories exactly once`() {
        val first = Client(null, 1)
        val second = Client(null, 2)
        PlayerRegistry.players[1] = first
        PlayerRegistry.players[2] = second
        first.inTrade = true
        second.inTrade = true
        first.trade_reqId = 2
        second.trade_reqId = 1
        TradeDuelSessionService.beginTradeSession(first, second)
        first.playerItems[0] = 996
        first.playerItemsN[0] = 10
        second.playerItems[0] = 101
        second.playerItemsN[0] = 1
        second.playerItems[1] = 101
        second.playerItemsN[1] = 1
        assertTrue(ExchangeRuntime.reserve(first, 0, 995, 5))
        assertTrue(ExchangeRuntime.reserve(second, 0, 100, 2))

        TradingService.decline(first)

        assertFalse(first.inTrade)
        assertFalse(second.inTrade)
        assertTrue(ExchangeRuntime.offers(first).isEmpty())
        assertTrue(ExchangeRuntime.offers(second).isEmpty())
        assertEquals(10, first.playerItemsN[0])
        assertEquals(listOf(101, 101), second.playerItems.take(2))

        TradingService.decline(first)
        assertEquals(10, first.playerItemsN[0])
        assertEquals(listOf(101, 101), second.playerItems.take(2))
    }

    @Test
    fun `full inventory does not prevent reservation cancellation`() {
        val client = Client(null, 1).apply { dbId = 10; inTrade = true }
        val partner = Client(null, 2).apply { dbId = 20; inTrade = true }
        client.playerItems.indices.forEach { slot ->
            client.playerItems[slot] = 101
            client.playerItemsN[slot] = 1
        }
        ExchangeRuntime.create(net.dodian.uber.game.api.plugin.social.ExchangeKind.TRADE, client, partner)
        assertTrue(ExchangeRuntime.reserve(client, 0, 100, 1))

        ExchangeRuntime.remove(client)

        assertTrue(ExchangeRuntime.offers(client).isEmpty())
        assertTrue(client.playerItems.all { it == 101 })
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
