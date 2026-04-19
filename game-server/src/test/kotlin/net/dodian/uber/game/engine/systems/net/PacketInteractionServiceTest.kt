package net.dodian.uber.game.engine.systems.net

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.tasking.GameTaskRuntime
import net.dodian.uber.game.engine.loop.GameCycleClock
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.AttackStartDedupeState
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.combat.CombatIntent
import net.dodian.uber.game.engine.systems.combat.CombatStartService
import net.dodian.uber.game.engine.systems.interaction.AttackPlayerIntent
import net.dodian.uber.game.engine.systems.interaction.InteractionProcessor
import net.dodian.uber.game.engine.systems.interaction.NpcInteractionIntent
import net.dodian.uber.game.engine.systems.interaction.PersonalPassageService
import net.dodian.uber.game.engine.systems.world.npc.NpcManager
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PacketInteractionServiceTest {
    private var previousNpcManager: NpcManager? = null

    @BeforeEach
    fun setUp() {
        previousNpcManager = Server.npcManager
        Server.npcManager = previousNpcManager ?: NpcManager()
        Server.npcManager.npcMap.clear()
        PlayerRegistry.playersOnline.clear()
        PlayerRegistry.initializeSlots()
    }

    @AfterEach
    fun tearDown() {
        GameTaskRuntime.clear()
        PlayerRegistry.playersOnline.clear()
        PlayerRegistry.initializeSlots()
        Server.npcManager = previousNpcManager
    }

    @Test
    fun `nearby npc click clears redundant walk queue`() {
        val client = clientAt(slot = 1, nameKey = 11L, x = 2728, y = 3348)
        val npc = npcAt(slot = 42, id = 3951, x = 2727, y = 3349)

        queueOneTileWalk(client, 2729, 3349)
        assertTrue(client.newWalkCmdSteps > 0)

        PacketInteractionService.handleNpcClick(client, opcode = 155, option = 1, npcIndex = npc.slot)

        assertEquals(0, client.newWalkCmdSteps)
        assertEquals(client.wQueueReadPtr, client.wQueueWritePtr)
        assertTrue(client.pendingInteraction != null)
    }

    @Test
    fun `clicking legends guard primes personal passage for blocked gate approach tile`() {
        val client = clientAt(slot = 11, nameKey = 111L, x = 2728, y = 3348)
        client.premium = true
        val npc = npcAt(slot = 52, id = 3951, x = 2730, y = 3349)

        PacketInteractionService.handleNpcClick(client, opcode = 155, option = 1, npcIndex = npc.slot)

        assertTrue(PersonalPassageService.canTraverse(client, 2728, 3348, 2729, 3349, 0))
    }

    @Test
    fun `non premium legends guard click does not prime personal passage`() {
        val client = clientAt(slot = 12, nameKey = 121L, x = 2728, y = 3348)
        client.premium = false
        val npc = npcAt(slot = 53, id = 3951, x = 2730, y = 3349)

        PacketInteractionService.handleNpcClick(client, opcode = 155, option = 1, npcIndex = npc.slot)

        assertFalse(PersonalPassageService.canTraverse(client, 2728, 3348, 2729, 3349, 0))
    }

    @Test
    fun `adjacent npc helper only clears walk when already in talk range`() {
        val client = clientAt(slot = 2, nameKey = 22L, x = 2728, y = 3348)
        val nearbyNpc = npcAt(slot = 43, id = 3951, x = 2727, y = 3349)
        val otherGuard = npcAt(slot = 44, id = 3951, x = 2730, y = 3349)

        assertTrue(PacketInteractionService.shouldClearRedundantWalkForNpcInteraction(client, nearbyNpc, option = 1))
        assertFalse(PacketInteractionService.shouldClearRedundantWalkForNpcInteraction(client, nearbyNpc, option = 5))
        assertFalse(PacketInteractionService.shouldClearRedundantWalkForNpcInteraction(client, otherGuard, option = 1))
    }

    @Test
    fun `legends guard front lane override allows both guard clicks from front tiles`() {
        val client = clientAt(slot = 13, nameKey = 131L, x = 2728, y = 3348)
        val westGuard = npcAt(slot = 54, id = 3951, x = 2727, y = 3349)
        val eastGuard = npcAt(slot = 55, id = 3951, x = 2730, y = 3349)

        assertTrue(InteractionProcessor.isLegendsGuardFrontLaneInteraction(client, westGuard, option = 1))
        assertTrue(InteractionProcessor.isLegendsGuardFrontLaneInteraction(client, eastGuard, option = 1))
        assertFalse(InteractionProcessor.isLegendsGuardFrontLaneInteraction(client, eastGuard, option = 5))
    }

    @Test
    fun `same-target npc attack packet is ignored when combat target is already active`() {
        val client = clientAt(slot = 14, nameKey = 141L, x = 3200, y = 3200)
        val npc = npcAt(slot = 56, id = 1, x = 3201, y = 3200)
        GameCycleClock.syncTo(100)
        CombatStartService.beginAttackNow(client, npc, CombatIntent.ATTACK_NPC)

        PacketInteractionService.handleNpcAttack(client, opcode = 72, npcIndex = npc.slot)
        assertTrue(client.pendingInteraction == null)
        assertEquals(npc.slot, client.combatEngagementState?.targetSlot)
    }

    @Test
    fun `same-target npc attack packet is ignored when cooldown is active and state is transiently missing`() {
        val client = clientAt(slot = 20, nameKey = 1410L, x = 3200, y = 3200)
        val npc = npcAt(slot = 60, id = 1, x = 3201, y = 3200)
        CombatStartService.beginAttackNow(client, npc, CombatIntent.ATTACK_NPC)
        client.combatTargetState = null
        client.combatTimer = 3
        client.attackStartDedupeState =
            AttackStartDedupeState(
                CombatIntent.ATTACK_NPC,
                Entity.Type.NPC,
                npc.slot,
                1L,
            )

        PacketInteractionService.handleNpcAttack(client, opcode = 72, npcIndex = npc.slot)

        assertTrue(client.pendingInteraction == null)
        assertTrue(client.target === npc)
    }

    @Test
    fun `same-target player attack packet is ignored when combat target is already active`() {
        val attacker = clientAt(slot = 15, nameKey = 151L, x = 3200, y = 3200)
        val victim = clientAt(slot = 16, nameKey = 161L, x = 3201, y = 3200)
        GameCycleClock.syncTo(200)
        CombatStartService.beginAttackNow(attacker, victim, CombatIntent.ATTACK_PLAYER)
        val before = attacker.combatCooldownState ?: error("missing combat cooldown")

        PacketInteractionService.handleAttackPlayer(attacker, opcode = 73, victimSlot = victim.slot)
        assertTrue(attacker.pendingInteraction == null)
        val after = attacker.combatCooldownState ?: error("missing combat cooldown")
        assertEquals(before.nextAttackCycle, after.nextAttackCycle)
        assertEquals(victim.slot, attacker.combatEngagementState?.targetSlot)
    }

    @Test
    fun `different target attack packet retargets immediately without resetting cooldown`() {
        val attacker = clientAt(slot = 23, nameKey = 1151L, x = 3200, y = 3200)
        val firstVictim = clientAt(slot = 24, nameKey = 1161L, x = 3201, y = 3200)
        val secondVictim = clientAt(slot = 25, nameKey = 1171L, x = 3202, y = 3200)

        GameCycleClock.syncTo(300)
        CombatStartService.beginAttackNow(attacker, firstVictim, CombatIntent.ATTACK_PLAYER)
        attacker.combatCooldownState =
            attacker.combatCooldownState?.copy(
                initialSwingConsumed = true,
                nextAttackCycle = 305L,
                lastAttackCycle = 299L,
            )
        attacker.combatTimer = 5

        PacketInteractionService.handleAttackPlayer(attacker, opcode = 73, victimSlot = secondVictim.slot)

        assertTrue(attacker.pendingInteraction == null)
        assertEquals(secondVictim.slot, attacker.combatEngagementState?.targetSlot)
        assertEquals(305L, attacker.combatCooldownState?.nextAttackCycle)
    }

    @Test
    fun `same-target player attack packet is ignored when cooldown is active and state is transiently missing`() {
        val attacker = clientAt(slot = 26, nameKey = 1510L, x = 3200, y = 3200)
        val victim = clientAt(slot = 27, nameKey = 1610L, x = 3201, y = 3200)
        CombatStartService.beginAttackNow(attacker, victim, CombatIntent.ATTACK_PLAYER)
        attacker.combatTargetState = null
        attacker.combatTimer = 2
        attacker.attackStartDedupeState =
            AttackStartDedupeState(
                CombatIntent.ATTACK_PLAYER,
                Entity.Type.PLAYER,
                victim.slot,
                1L,
            )

        PacketInteractionService.handleAttackPlayer(attacker, opcode = 73, victimSlot = victim.slot)

        assertTrue(attacker.pendingInteraction == null)
        assertTrue(attacker.target === victim)
    }

    @Test
    fun `same-target npc attack packet is ignored when matching attack is already pending`() {
        val client = clientAt(slot = 17, nameKey = 171L, x = 3200, y = 3200)
        val npc = npcAt(slot = 57, id = 3951, x = 3201, y = 3200)
        val pending = NpcInteractionIntent(opcode = 72, createdCycle = 1L, npcIndex = npc.slot, option = 5)
        client.pendingInteraction = pending

        PacketInteractionService.handleNpcAttack(client, opcode = 72, npcIndex = npc.slot)

        assertTrue(client.pendingInteraction === pending)
    }

    @Test
    fun `same-target player attack packet is ignored when matching attack is already pending`() {
        val attacker = clientAt(slot = 18, nameKey = 181L, x = 3200, y = 3200)
        val victim = clientAt(slot = 19, nameKey = 191L, x = 3201, y = 3200)
        val pending = AttackPlayerIntent(opcode = 73, createdCycle = 1L, victimIndex = victim.slot)
        attacker.pendingInteraction = pending

        PacketInteractionService.handleAttackPlayer(attacker, opcode = 73, victimSlot = victim.slot)

        assertTrue(attacker.pendingInteraction === pending)
    }

    private fun clientAt(slot: Int, nameKey: Long, x: Int, y: Int): Client {
        val client = Client(EmbeddedChannel(), slot)
        client.longName = nameKey
        client.playerName = "packet-interaction-$slot"
        client.isActive = true
        client.initialized = true
        client.disconnected = false
        client.pLoaded = true
        client.validClient = true
        client.dbId = nameKey.toInt()
        client.teleportTo(x, y, 0)
        primeMovementState(client)
        PlayerRegistry.playersOnline[nameKey] = client
        PlayerRegistry.players[slot] = client
        return client
    }

    private fun npcAt(slot: Int, id: Int, x: Int, y: Int): Npc {
        val npc = Npc(slot, id, Position(x, y, 0), 0)
        Server.npcManager.npcMap[slot] = npc
        return npc
    }

    private fun queueOneTileWalk(client: Client, targetX: Int, targetY: Int) {
        queueWalkPath(client, targetX, targetY, intArrayOf(0), intArrayOf(0))
    }

    private fun queueWalkPath(client: Client, firstStepXAbs: Int, firstStepYAbs: Int, deltasX: IntArray, deltasY: IntArray) {
        PacketWalkingService.handle(
            client,
            WalkRequest(
                opcode = 164,
                firstStepXAbs = firstStepXAbs,
                firstStepYAbs = firstStepYAbs,
                running = false,
                deltasX = deltasX,
                deltasY = deltasY,
            ),
        )
    }

    private fun primeMovementState(player: Client) {
        player.getNextPlayerMovement()
        player.postProcessing()
        player.clearUpdateFlags()
    }
}
