package net.dodian.uber.game.content.objects.impl.mining

import io.netty.channel.embedded.EmbeddedChannel
import java.lang.reflect.Proxy
import java.sql.ResultSet
import java.util.Arrays
import net.dodian.uber.game.Server
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.item.Item
import net.dodian.uber.game.model.item.ItemManager
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.runtime.task.GameTaskRuntime
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MiningServiceTest {

    @BeforeEach
    fun setUp() {
        ensureItemManager()
    }

    @AfterEach
    fun tearDown() {
        GameTaskRuntime.clear()
        PlayerHandler.playersOnline.clear()
    }

    @Test
    fun `resolve best pickaxe checks equipment and inventory`() {
        val client = activeClient(1, 101, miningLevel = 61)
        client.getEquipment()[Equipment.Slot.WEAPON.id] = 1265
        client.playerItems[0] = 1275 + 1

        val pickaxe = MiningService.resolveBestPickaxe(client)

        assertEquals(1275, pickaxe?.itemId)
    }

    @Test
    fun `speed formula matches old java behavior without boost`() {
        val client = activeClient(2, 102, miningLevel = 61)
        val rock = MiningDefinitions.rocksByObjectId.getValue(7458)
        val pickaxe = MiningDefinitions.pickaxeByItemId.getValue(1275)

        val delay = MiningService.computeMiningDelayMs(client, rock, pickaxe, boostRoll = 2)

        assertEquals((6000.0 / (1 + 0.42 + (61 / 256.0))).toLong(), delay)
    }

    @Test
    fun `dragon tier boost branch matches old logic`() {
        val client = activeClient(3, 103, miningLevel = 61)
        val rock = MiningDefinitions.rocksByObjectId.getValue(7458)
        val pickaxe = MiningDefinitions.pickaxeByItemId.getValue(11920)

        val boostedDelay = MiningService.computeMiningDelayMs(client, rock, pickaxe, boostRoll = 1)
        val normalDelay = MiningService.computeMiningDelayMs(client, rock, pickaxe, boostRoll = 2)

        assertEquals((((6000.0 - 600.0) / (1 + 0.8 + (61 / 256.0))).toLong()), boostedDelay)
        assertTrue(boostedDelay < normalDelay)
    }

    @Test
    fun `rune essence uses special rest threshold and no random gems`() {
        val rock = MiningDefinitions.rocksByObjectId.getValue(7471)

        assertEquals(1436, rock.oreItemId)
        assertFalse(rock.randomGemEligible)
        assertEquals(14, rock.restThreshold)
    }

    @Test
    fun `random gem chance uses glory modifier`() {
        val client = activeClient(4, 104, miningLevel = 61)
        client.getEquipment()[Equipment.Slot.NECK.id] = 1704

        assertEquals(128, MiningService.resolveRandomGemChance(client))

        client.getEquipment()[Equipment.Slot.NECK.id] = -1
        assertEquals(256, MiningService.resolveRandomGemChance(client))
    }

    @Test
    fun `random gem reward uses configured drop table`() {
        val client = activeClient(5, 105, miningLevel = 61)

        val gem = MiningService.tryAwardRandomGem(client, 256, roll = 1, gemIndex = 3)

        assertEquals(1621, gem)
        assertTrue(client.playerHasItem(1621))
    }

    @Test
    fun `mining starts a player task and state`() {
        val client = minerClient(6, 106)
        val rock = MiningDefinitions.rocksByObjectId.getValue(7451)

        val started = MiningService.startMining(client, rock, client.position.copy())

        assertTrue(started)
        assertNotNull(client.miningState)
        assertNotNull(client.miningTaskHandle)
    }

    @Test
    fun `mining success grants ore and xp`() {
        val client = minerClient(7, 107)
        val rock = MiningDefinitions.rocksByObjectId.getValue(7451)
        val startedXp = client.getExperience(Skill.MINING)

        MiningService.startMining(client, rock, client.position.copy())
        client.lastAction = 0L

        GameTaskRuntime.cycle()

        assertTrue(client.playerHasItem(436))
        assertTrue(client.getExperience(Skill.MINING) > startedXp)
        assertNotNull(client.miningState)
    }

    @Test
    fun `mining stops on full inventory`() {
        val client = minerClient(8, 108)
        val rock = MiningDefinitions.rocksByObjectId.getValue(7451)

        MiningService.startMining(client, rock, client.position.copy())
        Arrays.fill(client.playerItems, 1)
        client.lastAction = 0L

        GameTaskRuntime.cycle()

        assertNull(client.miningState)
        assertNull(client.miningTaskHandle)
    }

    @Test
    fun `mining stops on missing pickaxe`() {
        val client = minerClient(9, 109)
        val rock = MiningDefinitions.rocksByObjectId.getValue(7451)

        MiningService.startMining(client, rock, client.position.copy())
        client.getEquipment()[Equipment.Slot.WEAPON.id] = -1
        client.lastAction = 0L

        GameTaskRuntime.cycle()

        assertNull(client.miningState)
        assertNull(client.miningTaskHandle)
    }

    @Test
    fun `mining stops when player moves away`() {
        val client = minerClient(10, 110)
        val rock = MiningDefinitions.rocksByObjectId.getValue(7451)

        MiningService.startMining(client, rock, client.position.copy())
        client.miningState = client.miningState!!.copy(rockPosition = Position(client.position.x + 10, client.position.y + 10, client.position.z))

        GameTaskRuntime.cycle()

        assertNull(client.miningState)
    }

    @Test
    fun `reset action cancels mining task and clears state`() {
        val client = minerClient(11, 111)
        val rock = MiningDefinitions.rocksByObjectId.getValue(7451)

        MiningService.startMining(client, rock, client.position.copy())
        client.resetAction(true)

        assertNull(client.miningState)
        assertNull(client.miningTaskHandle)
    }

    private fun minerClient(slot: Int, dbId: Int): Client {
        val client = activeClient(slot, dbId, miningLevel = 61)
        client.getEquipment()[Equipment.Slot.WEAPON.id] = 1265
        return client
    }

    private fun activeClient(slot: Int, dbId: Int, miningLevel: Int): Client {
        val client = Client(EmbeddedChannel(), slot)
        client.dbId = dbId
        client.isActive = true
        client.disconnected = false
        client.setLevel(miningLevel, Skill.MINING)
        client.setExperience(1_000_000, Skill.MINING)
        Arrays.fill(client.getEquipment(), -1)
        PlayerHandler.playersOnline[slot.toLong()] = client
        return client
    }

    private fun ensureItemManager() {
        if (Server.itemManager == null) {
            Server.itemManager = ItemManager()
        }
        val manager = Server.itemManager
        manager.items[436] = fakeItem(436, "Copper ore")
        manager.items[1436] = fakeItem(1436, "Rune essence")
        manager.items[1621] = fakeItem(1621, "Emerald")
        manager.items[1704] = fakeItem(1704, "Amulet of glory")
        manager.items[1265] = fakeItem(1265, "Bronze pickaxe")
        manager.items[1275] = fakeItem(1275, "Rune pickaxe")
        manager.items[11920] = fakeItem(11920, "Dragon pickaxe")
    }

    private fun fakeItem(id: Int, name: String, stackable: Boolean = false): Item {
        val resultSet =
            Proxy.newProxyInstance(
                ResultSet::class.java.classLoader,
                arrayOf(ResultSet::class.java),
            ) { _, method, args ->
                val column = args?.firstOrNull() as? String
                when (method.name) {
                    "getInt" ->
                        when {
                            column == "id" -> id
                            column == "slot" -> 0
                            column == "full" -> 0
                            column == "shopSellValue" -> 1
                            column == "shopBuyValue" -> 1
                            column == "Alchemy" -> 1
                            column == "standAnim" -> 808
                            column == "walkAnim" -> 819
                            column == "runAnim" -> 824
                            column == "attackAnim" -> 806
                            column == "premium" -> 0
                            column != null && column.startsWith("bonus") -> 0
                            else -> 0
                        }

                    "getString" ->
                        when (column) {
                            "name" -> name.replace(" ", "_")
                            "description" -> ""
                            else -> null
                        }

                    "getBoolean" ->
                        when (column) {
                            "noteable" -> false
                            "tradeable" -> true
                            "twohanded" -> false
                            "stackable" -> stackable
                            else -> false
                        }

                    else -> throw UnsupportedOperationException("Unsupported ResultSet method: ${method.name}")
                }
            } as ResultSet

        return Item(resultSet)
    }
}
