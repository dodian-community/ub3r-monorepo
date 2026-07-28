package net.dodian.uber.game.engine.systems.combat

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.loop.GameCycleClock
import net.dodian.uber.game.engine.routing.WorldRouteService
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.world.npc.NpcManager
import net.dodian.uber.game.item.ItemManager
import net.dodian.uber.game.engine.config.SettingsLoader
import net.dodian.uber.game.engine.systems.net.PacketWalkingService
import net.dodian.uber.game.engine.systems.net.WalkRequest
import net.dodian.uber.game.testutil.RequiresCache
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CombatFollowTest {
    @BeforeEach
    fun setUp() {
        RequiresCache.assume()
        SettingsLoader.load()
        Server.npcManager = NpcManager()
        Server.itemManager = ItemManager(definitionLoader = { emptyMap() }, globalSpawnBootstrap = {})
        WorldRouteService.clear()
        WorldRouteService.allocateZone(3200, 3200, 0)
        GameCycleClock.syncTo(100)
    }

    @AfterEach
    fun tearDown() {
        WorldRouteService.clear()
        Server.npcManager = null
        Server.itemManager = null
    }

    @Test
    fun `npc combat auto-follow routes to npc boundary`() {
        val player = testClient(slot = 1, x = 3200, y = 3200)
        val npc = Npc(2, 1, Position(3202, 3200, 0), 0)
        Server.npcManager.npcMap[2] = npc

        // Request attack on NPC
        val requestResult = CombatCommandService.requestAttack(player, npc, CombatIntent.ATTACK_NPC)
        assertEquals(CombatCommandService.AttackRequestResult.STARTED, requestResult)

        GameCycleClock.advance()

        // Process combat tick
        CombatRuntimeService.process(player, GameCycleClock.currentCycle())

        // Verify player queued walking steps
        assertTrue(player.hasMovementRoute())
        val destination = requireNotNull(player.movementDestination())
        val finalX = destination.x
        val finalY = destination.y

        // The player should stand on the tile next to the NPC (3201, 3200)
        assertEquals(3201, finalX)
        assertEquals(3200, finalY)
    }

    @Test
    fun `walk packet opcode 98 in same tick as attack packet does not cancel combat`() {
        val player = testClient(slot = 1, x = 3200, y = 3200)
        val npc = Npc(2, 1, Position(3202, 3200, 0), 0)
        Server.npcManager.npcMap[2] = npc

        // Simulate receiving Attack packet (opcode 72)
        CombatStartService.beginAttackNow(player, npc, CombatIntent.ATTACK_NPC)
        
        // Simulate receiving Walk packet (opcode 98)
        val walkRequest = WalkRequest(98, 3202, 3200, false, intArrayOf(0), intArrayOf(0))
        PacketWalkingService.handle(player, walkRequest)

        // Process combat tick
        CombatRuntimeService.process(player, GameCycleClock.currentCycle())

        // Verify player STILL has combat target!
        assertTrue(CombatRuntimeService.hasActiveCombat(player))
    }

    private fun testClient(slot: Int, x: Int, y: Int): Client {
        val client = Client(EmbeddedChannel(), slot)
        client.isActive = true
        client.initialized = true
        client.disconnected = false
        client.pLoaded = true
        client.teleportTo(x, y, 0)
        return client
    }
}
