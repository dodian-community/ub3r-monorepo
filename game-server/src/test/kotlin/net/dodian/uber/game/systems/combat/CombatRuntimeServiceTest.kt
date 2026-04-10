package net.dodian.uber.game.systems.combat

import io.netty.channel.embedded.EmbeddedChannel
import kotlin.math.abs
import net.dodian.uber.game.engine.loop.GameCycleClock
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.pathing.collision.CollisionManager
import net.dodian.uber.game.systems.world.player.PlayerRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CombatRuntimeServiceTest {
    @AfterEach
    fun tearDown() {
        PlayerRegistry.playersOnline.clear()
        CollisionManager.global().clear()
        GameCycleClock.syncTo(0)
    }

    @Test
    fun `melee combat auto-follow routes around blocked tile instead of using direct run cords`() {
        val attacker = testClient(slot = 41, nameKey = 3041L, x = 3200, y = 3200)
        val target = testClient(slot = 42, nameKey = 3042L, x = 3202, y = 3200)
        CollisionManager.global().flagSolid(3201, 3200, 0)

        GameCycleClock.syncTo(100)
        CombatStartService.beginAttackNow(attacker, target, CombatIntent.ATTACK_PLAYER)
        GameCycleClock.advance()

        CombatRuntimeService.process(attacker, GameCycleClock.currentCycle())

        assertTrue(attacker.newWalkCmdSteps > 0)
        assertTrue(attacker.newWalkCmdIsRunning)
        assertEquals(32768 + target.slot, attacker.faceTarget)

        val waypoints = absoluteWaypoints(attacker)
        assertFalse(waypoints.contains(3201 to 3200))
        val destination = waypoints.last()
        assertTrue(abs(destination.first - target.position.x) <= 1)
        assertTrue(abs(destination.second - target.position.y) <= 1)
    }

    @Test
    fun `melee combat auto-follow prefers tile behind moving player target`() {
        val attacker = testClient(slot = 43, nameKey = 3043L, x = 3200, y = 3200)
        val target = testClient(slot = 44, nameKey = 3044L, x = 3202, y = 3200)
        target.setLastWalkDelta(1, 0)

        GameCycleClock.syncTo(200)
        CombatStartService.beginAttackNow(attacker, target, CombatIntent.ATTACK_PLAYER)
        GameCycleClock.advance()

        CombatRuntimeService.process(attacker, GameCycleClock.currentCycle())

        val destination = absoluteWaypoints(attacker).last()
        assertEquals(3201, destination.first)
        assertEquals(3200, destination.second)
    }

    private fun testClient(
        slot: Int,
        nameKey: Long,
        x: Int,
        y: Int,
    ): Client {
        val client = Client(EmbeddedChannel(), slot)
        client.longName = nameKey
        client.playerName = "player-$slot"
        client.isActive = true
        client.initialized = true
        client.disconnected = false
        client.pLoaded = true
        client.dbId = nameKey.toInt()
        client.teleportTo(x, y, 0)
        PlayerRegistry.playersOnline[nameKey] = client
        return client
    }

    private fun absoluteWaypoints(player: Client): List<Pair<Int, Int>> {
        val baseX = player.mapRegionX * 8
        val baseY = player.mapRegionY * 8
        return (0 until player.newWalkCmdSteps)
            .map { (player.newWalkCmdX[it] + baseX) to (player.newWalkCmdY[it] + baseY) }
    }
}


