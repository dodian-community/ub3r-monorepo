package net.dodian.uber.game.npc

import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.systems.world.npc.NpcManager
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import net.dodian.uber.game.testutil.RequiresCache

class NpcCombatRegistryTest {
    private var previousNpcManager: NpcManager? = null

    @BeforeEach
    fun setUp() {
        RequiresCache.assume()
        previousNpcManager = Server.npcManager
        Server.npcManager = NpcManager()
    }

    @AfterEach
    fun tearDown() {
        Server.npcManager = previousNpcManager
    }

    @Test
    fun `family registration dispatches every explicitly declared id`() {
        val attacked = mutableListOf<Int>()
        val profile = NpcCombatProfile(NpcAttackHandler { npc -> attacked += npc.id; true })
        NpcCombatRegistry.registerFamily("test.family.dispatch", intArrayOf(65_000, 65_001), profile)

        assertTrue(NpcCombatRegistry.handleAttack(Npc(1, 65_000, Position(3200, 3200, 0), 0)))
        assertTrue(NpcCombatRegistry.handleAttack(Npc(2, 65_001, Position(3200, 3200, 0), 0)))
        assertEquals(listOf(65_000, 65_001), attacked)
        assertEquals("test.family.dispatch", NpcCombatRegistry.ownerOf(65_001))
    }

    @Test
    fun `duplicate id registration is rejected at startup`() {
        NpcCombatRegistry.registerFamily(
            "test.family.first",
            intArrayOf(65_002),
            NpcCombatProfile(NpcAttackHandler { true }),
        )

        assertThrows(IllegalArgumentException::class.java) {
            NpcCombatRegistry.registerFamily(
                "test.family.second",
                intArrayOf(65_002),
                NpcCombatProfile(NpcAttackHandler { true }),
            )
        }
    }

    @Test
    fun `defence hook can alter damage and suppress the fallback reaction`() {
        NpcCombatRegistry.registerFamily(
            "test.family.defence",
            intArrayOf(65_003),
            NpcCombatProfile(
                attack = NpcAttackHandler { true },
                defend = { context ->
                    context.damage /= 2
                    context.suppressDefaultReaction = true
                },
            ),
        )
        val npc = Npc(3, 65_003, Position(3200, 3200, 0), 0)
        val result = NpcCombatRegistry.applyDefence(
            npc,
            Client(null, 1),
            11,
            Entity.hitType.STANDARD,
        )

        assertEquals(5, result.damage)
        assertTrue(result.suppressDefaultReaction)
        assertFalse(NpcCombatRegistry.handleAttack(Npc(4, 64_999, Position(3200, 3200, 0), 0)))
    }
}
