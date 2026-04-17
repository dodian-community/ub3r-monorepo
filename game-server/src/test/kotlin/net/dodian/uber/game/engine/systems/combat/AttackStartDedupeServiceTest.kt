package net.dodian.uber.game.engine.systems.combat

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.engine.systems.combat.AttackStartDedupeService.Decision
import net.dodian.uber.game.engine.systems.interaction.AttackPlayerIntent
import net.dodian.uber.game.engine.systems.interaction.NpcInteractionIntent
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.player.AttackStartDedupeState
import net.dodian.uber.game.model.entity.player.Client
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AttackStartDedupeServiceTest {
    @Test
    fun `returns duplicate pending for same npc attack while pending`() {
        val player = testClient(slot = 1, nameKey = 501L)
        player.pendingInteraction = NpcInteractionIntent(opcode = 72, createdCycle = 100L, npcIndex = 44, option = 5)

        val decision =
            AttackStartDedupeService.shouldAcceptAttackStart(
                player = player,
                intent = CombatIntent.ATTACK_NPC,
                targetType = Entity.Type.NPC,
                targetSlot = 44,
                cycle = 100L,
            )

        assertEquals(Decision.DUPLICATE_PENDING, decision)
    }

    @Test
    fun `returns duplicate pending for same player attack while pending`() {
        val player = testClient(slot = 2, nameKey = 502L)
        player.pendingInteraction = AttackPlayerIntent(opcode = 73, createdCycle = 220L, victimIndex = 9)

        val decision =
            AttackStartDedupeService.shouldAcceptAttackStart(
                player = player,
                intent = CombatIntent.ATTACK_PLAYER,
                targetType = Entity.Type.PLAYER,
                targetSlot = 9,
                cycle = 220L,
            )

        assertEquals(Decision.DUPLICATE_PENDING, decision)
    }

    @Test
    fun `active combat ownership no longer lives in dedupe service`() {
        val player = testClient(slot = 3, nameKey = 503L)
        player.combatTargetState =
            CombatTargetState(
                intent = CombatIntent.ATTACK_NPC,
                targetType = Entity.Type.NPC,
                targetSlot = 55,
                startedCycle = 300L,
                lastInRangeConfirmationCycle = 300L,
                attackStyleAtStart = 0,
                initialSwingConsumed = true,
                nextAttackCycle = 304L,
                lastAttackCycle = 302L,
                autoFollowEnabled = true,
                lastFollowCycle = 302L,
                lastFollowTargetX = 3200,
                lastFollowTargetY = 3200,
                lastFollowTargetDeltaX = 0,
                lastFollowTargetDeltaY = 0,
            )

        val decision =
            AttackStartDedupeService.shouldAcceptAttackStart(
                player = player,
                intent = CombatIntent.ATTACK_NPC,
                targetType = Entity.Type.NPC,
                targetSlot = 55,
                cycle = 305L,
            )

        assertEquals(Decision.ACCEPT, decision)
    }

    @Test
    fun `locked target without pending or window does not block accept`() {
        val player = testClient(slot = 31, nameKey = 531L)
        val target = testClient(slot = 32, nameKey = 532L)
        player.target = target
        player.combatTargetState = null
        player.combatTimer = 0

        val decision =
            AttackStartDedupeService.shouldAcceptAttackStart(
                player = player,
                intent = CombatIntent.ATTACK_PLAYER,
                targetType = Entity.Type.PLAYER,
                targetSlot = target.slot,
                cycle = 1000L,
            )

        assertEquals(Decision.ACCEPT, decision)
    }

    @Test
    fun `cooldown ownership no longer lives in dedupe service`() {
        val player = testClient(slot = 33, nameKey = 533L)
        player.combatTargetState =
            CombatTargetState(
                intent = CombatIntent.ATTACK_NPC,
                targetType = Entity.Type.NPC,
                targetSlot = 77,
                startedCycle = 1200L,
                lastInRangeConfirmationCycle = 1200L,
                attackStyleAtStart = 0,
                initialSwingConsumed = true,
                nextAttackCycle = 1206L,
                lastAttackCycle = 1202L,
                autoFollowEnabled = true,
                lastFollowCycle = 1202L,
                lastFollowTargetX = 3200,
                lastFollowTargetY = 3200,
                lastFollowTargetDeltaX = 0,
                lastFollowTargetDeltaY = 0,
            )

        val decision =
            AttackStartDedupeService.shouldAcceptAttackStart(
                player = player,
                intent = CombatIntent.ATTACK_NPC,
                targetType = Entity.Type.NPC,
                targetSlot = 77,
                cycle = 1203L,
            )

        assertEquals(Decision.ACCEPT, decision)
    }

    @Test
    fun `combat timer alone does not block accept in dedupe service`() {
        val player = testClient(slot = 34, nameKey = 534L)
        val target = testClient(slot = 35, nameKey = 535L)
        player.target = target
        player.combatTargetState = null
        player.combatTimer = 3

        val decision =
            AttackStartDedupeService.shouldAcceptAttackStart(
                player = player,
                intent = CombatIntent.ATTACK_PLAYER,
                targetType = Entity.Type.PLAYER,
                targetSlot = target.slot,
                cycle = 1302L,
            )

        assertEquals(Decision.ACCEPT, decision)
    }

    @Test
    fun `returns duplicate window in same and adjacent cycle then accepts after`() {
        val player = testClient(slot = 4, nameKey = 504L)
        player.attackStartDedupeState =
            AttackStartDedupeState(
                CombatIntent.ATTACK_PLAYER,
                Entity.Type.PLAYER,
                12,
                500L,
            )

        val sameCycle =
            AttackStartDedupeService.shouldAcceptAttackStart(
                player = player,
                intent = CombatIntent.ATTACK_PLAYER,
                targetType = Entity.Type.PLAYER,
                targetSlot = 12,
                cycle = 500L,
            )
        val adjacentCycle =
            AttackStartDedupeService.shouldAcceptAttackStart(
                player = player,
                intent = CombatIntent.ATTACK_PLAYER,
                targetType = Entity.Type.PLAYER,
                targetSlot = 12,
                cycle = 501L,
            )
        val afterWindow =
            AttackStartDedupeService.shouldAcceptAttackStart(
                player = player,
                intent = CombatIntent.ATTACK_PLAYER,
                targetType = Entity.Type.PLAYER,
                targetSlot = 12,
                cycle = 502L,
            )

        assertEquals(Decision.DUPLICATE_WINDOW, sameCycle)
        assertEquals(Decision.DUPLICATE_WINDOW, adjacentCycle)
        assertEquals(Decision.ACCEPT, afterWindow)
        assertEquals(502L, player.attackStartDedupeState?.acceptedCycle)
    }

    @Test
    fun `accepts different target within one tick and updates state`() {
        val player = testClient(slot = 5, nameKey = 505L)
        player.attackStartDedupeState =
            AttackStartDedupeState(
                CombatIntent.ATTACK_NPC,
                Entity.Type.NPC,
                30,
                700L,
            )

        val decision =
            AttackStartDedupeService.shouldAcceptAttackStart(
                player = player,
                intent = CombatIntent.ATTACK_NPC,
                targetType = Entity.Type.NPC,
                targetSlot = 31,
                cycle = 701L,
            )

        assertEquals(Decision.ACCEPT, decision)
        assertEquals(31, player.attackStartDedupeState?.targetSlot)
        assertEquals(701L, player.attackStartDedupeState?.acceptedCycle)
    }

    @Test
    fun `ignore pending intent allows runtime boundary to evaluate active state instead`() {
        val player = testClient(slot = 36, nameKey = 536L)
        player.pendingInteraction = AttackPlayerIntent(opcode = 73, createdCycle = 2000L, victimIndex = 44)

        val decision =
            AttackStartDedupeService.shouldAcceptAttackStart(
                player = player,
                intent = CombatIntent.ATTACK_PLAYER,
                targetType = Entity.Type.PLAYER,
                targetSlot = 44,
                cycle = 2000L,
                ignorePendingIntent = true,
            )

        assertEquals(Decision.ACCEPT, decision)
    }

    @Test
    fun `duplicate window decisions do not update last accepted state`() {
        val player = testClient(slot = 37, nameKey = 537L)
        player.attackStartDedupeState =
            AttackStartDedupeState(
                CombatIntent.ATTACK_NPC,
                Entity.Type.NPC,
                11,
                3004L,
            )

        val decision =
            AttackStartDedupeService.shouldAcceptAttackStart(
                player = player,
                intent = CombatIntent.ATTACK_NPC,
                targetType = Entity.Type.NPC,
                targetSlot = 11,
                cycle = 3005L,
            )

        assertEquals(Decision.DUPLICATE_WINDOW, decision)
        assertEquals(3004L, player.attackStartDedupeState?.acceptedCycle)
    }

    private fun testClient(slot: Int, nameKey: Long): Client {
        val client = Client(EmbeddedChannel(), slot)
        client.longName = nameKey
        client.playerName = "attack-start-dedupe-$slot"
        client.isActive = true
        client.initialized = true
        client.disconnected = false
        client.pLoaded = true
        client.validClient = true
        client.dbId = nameKey.toInt()
        return client
    }
}
