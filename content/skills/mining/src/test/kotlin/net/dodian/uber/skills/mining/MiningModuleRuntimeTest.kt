package net.dodian.uber.skills.mining

import net.dodian.uber.game.api.plugin.skills.SkillObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillObjectRef
import net.dodian.uber.game.api.plugin.skills.SkillPosition
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.testkit.FakeSkillPlayer
import net.dodian.uber.skills.testkit.LiveSkillModuleFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private const val COPPER_ROCK_OBJECT_ID = 7451
private const val BRONZE_PICKAXE = 1265
private const val RUNE_PICKAXE = 1275
private const val COPPER_ORE = 436
private const val RUNE_ESSENCE_OBJECT_ID = 7471
private const val RUNE_ESSENCE_ITEM_ID = 1436
private const val FIRST_GEM_IN_TABLE = 1623

private fun rockRef(id: Int = COPPER_ROCK_OBJECT_ID) = SkillObjectRef(id, SkillPosition(3200, 3200, 0))

class MiningModuleRuntimeTest {
    @Test
    fun `descriptor and manifest are wired consistently`() {
        LiveSkillModuleFixture.requirePlugin(MiningModule.descriptor.id, Skill.MINING)
        assertEquals(MiningModule.descriptor.id, MiningModule.contentManifest.id)
    }

    @Test
    fun `data loads all 10 rocks and 9 pickaxes`() {
        assertEquals(10, MiningModule.rocks.size)
        assertEquals(9, MiningModule.pickaxes.size)
    }

    @Test
    fun `yanille mine rock 11390 routes to iron mining`() {
        val rock = MiningModule.rocks.firstOrNull { 11390 in it.objectIds }
        assertEquals("iron", rock?.name)
        assertEquals(440, rock?.oreItemId)
    }

    @Test
    fun `mining a copper rock eventually yields ore and a bonus gem`() {
        val binding = MiningModule.definition.objectBindings.single { COPPER_ROCK_OBJECT_ID in it.objectIds }
        val player = FakeSkillPlayer(mapOf(BRONZE_PICKAXE to 1)).apply { setLevel(Skill.MINING, 1) }

        val handled = binding.handler(SkillObjectInteraction(player, 1, rockRef()))
        assertTrue(handled)
        assertEquals(listOf("You swing your pick at the rock..."), player.messages)

        player.advanceTicks(6)

        assertTrue(player.amount(COPPER_ORE) >= 1, "expected at least one copper ore mined")
        assertTrue(player.skills.experience(Skill.MINING) >= 110)
        // FakeSkillPlayer's random always rolls the best outcome, so a gem-eligible rock always awards one.
        assertTrue(player.amount(FIRST_GEM_IN_TABLE) >= 1, "expected a bonus gem")
        assertEquals(COPPER_ORE, player.gatheringLogs.first().first)
        assertEquals("Mining", player.gatheringLogs.first().third)
    }

    @Test
    fun `rune essence never shows a mine message and is never gem eligible`() {
        val binding = MiningModule.definition.objectBindings.single { RUNE_ESSENCE_OBJECT_ID in it.objectIds }
        val player = FakeSkillPlayer(mapOf(BRONZE_PICKAXE to 1)).apply { setLevel(Skill.MINING, 1) }

        binding.handler(SkillObjectInteraction(player, 1, rockRef(RUNE_ESSENCE_OBJECT_ID)))
        player.advanceTicks(5)

        assertTrue(player.amount(RUNE_ESSENCE_ITEM_ID) >= 1, "expected essence mined")
        assertFalse(player.messages.any { it.startsWith("You mine some") })
        assertFalse(player.messages.any { it.contains("inside the rock") })
    }

    @Test
    fun `no pickaxe refuses to start`() {
        val binding = MiningModule.definition.objectBindings.single { COPPER_ROCK_OBJECT_ID in it.objectIds }
        val player = FakeSkillPlayer()

        binding.handler(SkillObjectInteraction(player, 1, rockRef()))

        assertEquals(listOf("You need a pickaxe in which you got the required mining level for."), player.messages)
        assertNull(player.activeActionName())
    }

    @Test
    fun `insufficient level for the rock refuses to start`() {
        val runiteRock = MiningModule.rocks.single { it.name == "runite" }
        val binding = MiningModule.definition.objectBindings.single { runiteRock.objectIds.first() in it.objectIds }
        val player = FakeSkillPlayer(mapOf(BRONZE_PICKAXE to 1)).apply { setLevel(Skill.MINING, 1) }

        binding.handler(SkillObjectInteraction(player, 1, rockRef(runiteRock.objectIds.first())))

        assertEquals(
            listOf("You need a mining level of ${runiteRock.requiredLevel} to mine this rock"),
            player.messages,
        )
        assertNull(player.activeActionName())
    }

    @Test
    fun `mining inside the Tzhaar cave is blocked`() {
        val binding = MiningModule.definition.objectBindings.single { COPPER_ROCK_OBJECT_ID in it.objectIds }
        val player = FakeSkillPlayer(mapOf(BRONZE_PICKAXE to 1)).apply {
            setLevel(Skill.MINING, 99)
            positionValue = SkillPosition(2450, 5150, 0)
        }

        binding.handler(SkillObjectInteraction(player, 1, rockRef()))

        assertEquals(listOf("You can not mine here or the Tzhaar's will be angry!"), player.messages)
        assertNull(player.activeActionName())
    }

    @Test
    fun `walking away from the rock stops the action`() {
        val binding = MiningModule.definition.objectBindings.single { COPPER_ROCK_OBJECT_ID in it.objectIds }
        val player = FakeSkillPlayer(mapOf(BRONZE_PICKAXE to 1)).apply { setLevel(Skill.MINING, 99) }

        binding.handler(SkillObjectInteraction(player, 1, rockRef()))
        assertEquals("mining", player.activeActionName())

        player.withinBoundaryOverride = false
        player.advanceTicks(1)

        assertEquals("You moved too far away.", player.messages.last())
        assertNull(player.activeActionName())
    }

    @Test
    fun `resolveBestPickaxe prefers the highest tier the player has`() {
        val player = FakeSkillPlayer(mapOf(BRONZE_PICKAXE to 1, RUNE_PICKAXE to 1)).apply { setLevel(Skill.MINING, 99) }
        assertEquals(RUNE_PICKAXE, MiningModule.resolveBestPickaxe(player)?.itemId)
    }

    @Test
    fun `stopAction halts an in-progress mining trip`() {
        val binding = MiningModule.definition.objectBindings.single { COPPER_ROCK_OBJECT_ID in it.objectIds }
        val player = FakeSkillPlayer(mapOf(BRONZE_PICKAXE to 1)).apply { setLevel(Skill.MINING, 1) }

        binding.handler(SkillObjectInteraction(player, 1, rockRef()))
        assertEquals("mining", player.activeActionName())

        MiningModule.stopAction(player)
        assertNull(player.activeActionName())

        val oreBefore = player.amount(COPPER_ORE)
        player.advanceTicks(10)
        assertEquals(oreBefore, player.amount(COPPER_ORE))
    }
}
