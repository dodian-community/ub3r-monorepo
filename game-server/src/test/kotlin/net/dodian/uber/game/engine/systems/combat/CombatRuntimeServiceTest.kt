package net.dodian.uber.game.engine.systems.combat

import io.netty.channel.embedded.EmbeddedChannel
import kotlin.math.abs
import net.dodian.uber.game.engine.loop.GameCycleClock
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.combat.CombatCommandService.AttackRequestResult
import net.dodian.uber.game.engine.systems.net.PacketWalkingService
import net.dodian.uber.game.engine.systems.net.WalkRequest
import net.dodian.uber.game.engine.systems.pathing.collision.CollisionManager
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CombatRuntimeServiceTest {
    @AfterEach
    fun tearDown() {
        PlayerRegistry.playersOnline.clear()
        PlayerRegistry.initializeSlots()
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

    @Test
    fun `same target re-click keeps attack cadence state`() {
        val attacker = testClient(slot = 45, nameKey = 3045L, x = 3200, y = 3200)
        val target = testClient(slot = 46, nameKey = 3046L, x = 3201, y = 3200)

        GameCycleClock.syncTo(300)
        assertEquals(
            AttackRequestResult.STARTED,
            CombatCommandService.requestAttack(attacker, target, CombatIntent.ATTACK_PLAYER),
        )
        val initialEngagement = attacker.combatEngagementState ?: error("missing initial combat engagement")
        val initialCooldown = attacker.combatCooldownState ?: error("missing initial combat cooldown")

        GameCycleClock.advance()
        CombatStartService.canPerformAttackTick(attacker)
        val afterSwingGate = attacker.combatCooldownState ?: error("missing gated combat cooldown")
        assertTrue(afterSwingGate.initialSwingConsumed)

        assertEquals(
            AttackRequestResult.REFRESHED_SAME_TARGET,
            CombatCommandService.requestAttack(attacker, target, CombatIntent.ATTACK_PLAYER),
        )
        val afterReclickEngagement = attacker.combatEngagementState ?: error("missing reclick engagement")
        val afterReclickCooldown = attacker.combatCooldownState ?: error("missing reclick cooldown")

        assertEquals(initialEngagement.startedCycle, afterReclickEngagement.startedCycle)
        assertEquals(initialCooldown.nextAttackCycle, afterReclickCooldown.nextAttackCycle)
        assertTrue(afterReclickCooldown.initialSwingConsumed)
    }

    @Test
    fun `different target re-click switches target but preserves cooldown cadence`() {
        val attacker = testClient(slot = 47, nameKey = 3047L, x = 3200, y = 3200)
        val firstTarget = testClient(slot = 48, nameKey = 3048L, x = 3201, y = 3200)
        val secondTarget = testClient(slot = 49, nameKey = 3049L, x = 3202, y = 3200)

        GameCycleClock.syncTo(400)
        CombatCommandService.requestAttack(attacker, firstTarget, CombatIntent.ATTACK_PLAYER)
        attacker.combatCooldownState =
            attacker.combatCooldownState?.copy(
                initialSwingConsumed = true,
                nextAttackCycle = 404L,
                lastAttackCycle = 399L,
            )
        attacker.combatTimer = 4

        assertEquals(
            AttackRequestResult.RETARGETED,
            CombatCommandService.requestAttack(attacker, secondTarget, CombatIntent.ATTACK_PLAYER),
        )
        val engagement = attacker.combatEngagementState ?: error("missing retargeted engagement")
        val cooldown = attacker.combatCooldownState ?: error("missing retargeted cooldown")
        assertEquals(secondTarget.slot, engagement.targetSlot)
        assertEquals(404L, cooldown.nextAttackCycle)
        assertTrue(cooldown.initialSwingConsumed)
    }

    @Test
    fun `walk cancel clears engagement but preserves cooldown for later re-engage`() {
        val attacker = testClient(slot = 50, nameKey = 3050L, x = 3200, y = 3200)
        val target = testClient(slot = 51, nameKey = 3051L, x = 3201, y = 3200)

        GameCycleClock.syncTo(500)
        CombatCommandService.requestAttack(attacker, target, CombatIntent.ATTACK_PLAYER)
        attacker.combatCooldownState =
            attacker.combatCooldownState?.copy(
                initialSwingConsumed = true,
                nextAttackCycle = 504L,
                lastAttackCycle = 499L,
            )
        attacker.combatTimer = 4

        PacketWalkingService.handle(
            attacker,
            WalkRequest(
                opcode = 164,
                firstStepXAbs = 3203,
                firstStepYAbs = 3200,
                running = false,
                deltasX = intArrayOf(0),
                deltasY = intArrayOf(0),
            ),
        )

        assertTrue(attacker.combatEngagementState == null)
        val preservedCooldown = attacker.combatCooldownState ?: error("missing preserved cooldown")
        assertEquals(504L, preservedCooldown.nextAttackCycle)

        assertEquals(
            AttackRequestResult.STARTED_DURING_COOLDOWN,
            CombatCommandService.requestAttack(attacker, target, CombatIntent.ATTACK_PLAYER),
        )
        val reengaged = attacker.combatCooldownState ?: error("missing re-engaged cooldown")
        assertEquals(504L, reengaged.nextAttackCycle)
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
        PlayerRegistry.players[slot] = client
        return client
    }

    private fun absoluteWaypoints(player: Client): List<Pair<Int, Int>> {
        val baseX = player.mapRegionX * 8
        val baseY = player.mapRegionY * 8
        return (0 until player.newWalkCmdSteps)
            .map { (player.newWalkCmdX[it] + baseX) to (player.newWalkCmdY[it] + baseY) }
    }
}
