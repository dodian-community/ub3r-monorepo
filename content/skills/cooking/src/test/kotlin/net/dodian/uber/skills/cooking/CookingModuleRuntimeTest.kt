package net.dodian.uber.skills.cooking

import net.dodian.uber.game.api.plugin.skills.SkillItemOnObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillObjectRef
import net.dodian.uber.game.api.plugin.skills.SkillPosition
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.api.SkillMultiSelection
import net.dodian.uber.skills.testkit.FakeSkillPlayer
import net.dodian.uber.skills.testkit.LiveSkillModuleFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private const val RANGE_OBJECT_ID = 26181
private const val RAW_SHRIMP = 317
private const val COOKED_SHRIMP = 315
private const val BURNT_SHRIMP = 323

private fun rangeRef() = SkillObjectRef(RANGE_OBJECT_ID, SkillPosition(3212, 3214, 0))

class CookingModuleRuntimeTest {
    @Test
    fun `descriptor and manifest are wired consistently`() {
        LiveSkillModuleFixture.requirePlugin(CookingModule.descriptor.id, Skill.COOKING)
        assertEquals(CookingModule.descriptor.id, CookingModule.contentManifest.id)
    }

    @Test
    fun `seaweed on range burns the whole stack into ash`() {
        val binding = CookingModule.definition.itemOnObjectBindings.single { RANGE_OBJECT_ID in it.objectIds }
        val player = FakeSkillPlayer(mapOf(401 to 5))

        val handled = binding.handler(SkillItemOnObjectInteraction(player, rangeRef(), 401, 0, 3214))

        assertTrue(handled)
        assertEquals(0, player.amount(401))
        assertEquals(5, player.amount(1781))
        assertEquals(listOf("You burn all your seaweed into ashes."), player.messages)
    }

    @Test
    fun `cooking a single raw item starts immediately without a chatbox`() {
        val binding = CookingModule.definition.itemOnObjectBindings.single { RANGE_OBJECT_ID in it.objectIds }
        val player = FakeSkillPlayer(mapOf(RAW_SHRIMP to 1)).apply { setLevel(Skill.COOKING, 99) }

        binding.handler(SkillItemOnObjectInteraction(player, rangeRef(), RAW_SHRIMP, 0, 3214))
        assertEquals(SkillPosition(3212, 3214, 0), player.anchor)
        player.advanceTicks(3)

        // High cooking level relative to shrimp's burn roll base (30) guarantees success.
        assertEquals(0, player.amount(RAW_SHRIMP))
        assertEquals(1, player.amount(COOKED_SHRIMP))
        assertEquals(0, player.amount(BURNT_SHRIMP))
        assertEquals(150, player.skills.experience(Skill.COOKING))
        assertNull(player.activeActionName())
    }

    @Test
    fun `low cooking level guarantees a burn`() {
        val player = FakeSkillPlayer(mapOf(RAW_SHRIMP to 1)).apply { setLevel(Skill.COOKING, 1) }

        CookingModule.startCooking(player, RAW_SHRIMP, 1)
        player.advanceTicks(3)

        assertEquals(0, player.amount(RAW_SHRIMP))
        assertEquals(0, player.amount(COOKED_SHRIMP))
        assertEquals(1, player.amount(BURNT_SHRIMP))
        assertEquals(0, player.skills.experience(Skill.COOKING))
    }

    @Test
    fun `cooking a stack of raw items opens the quantity chatbox then cooks the chosen amount`() {
        val binding = CookingModule.definition.itemOnObjectBindings.single { RANGE_OBJECT_ID in it.objectIds }
        val player = FakeSkillPlayer(mapOf(RAW_SHRIMP to 4)).apply { setLevel(Skill.COOKING, 99) }

        binding.handler(SkillItemOnObjectInteraction(player, rangeRef(), RAW_SHRIMP, 0, 3214))
        val menu = player.production.pending()!!
        assertTrue(player.production.select(SkillMultiSelection(menu.key, menu.entries.single().recipe.key, 3)))
        player.advanceTicks(9)

        assertEquals(1, player.amount(RAW_SHRIMP))
        assertEquals(3, player.amount(COOKED_SHRIMP))
        assertEquals(450, player.skills.experience(Skill.COOKING))
        assertNull(player.activeActionName())
    }

    @Test
    fun `object click cooks the first cookable item in the inventory`() {
        val binding = CookingModule.definition.objectBindings.single { RANGE_OBJECT_ID in it.objectIds }
        val player = FakeSkillPlayer(mapOf(2134 to 1)).apply { setLevel(Skill.COOKING, 99) }

        val handled = binding.handler(SkillObjectInteraction(player, 1, rangeRef()))
        assertTrue(handled)
        player.advanceTicks(3)

        assertEquals(0, player.amount(2134))
        assertEquals(1, player.amount(2142))
    }

    @Test
    fun `object click with nothing cookable reports there is nothing to cook`() {
        val binding = CookingModule.definition.objectBindings.single { RANGE_OBJECT_ID in it.objectIds }
        val player = FakeSkillPlayer()

        binding.handler(SkillObjectInteraction(player, 1, rangeRef()))

        assertEquals(listOf("You don't have anything to cook."), player.messages)
    }

    @Test
    fun `stopAction cancels an in-progress cook and no further items are cooked`() {
        val player = FakeSkillPlayer(mapOf(RAW_SHRIMP to 5)).apply { setLevel(Skill.COOKING, 99) }

        CookingModule.startCooking(player, RAW_SHRIMP, 5)
        player.advanceTicks(1)
        assertEquals("cooking", player.activeActionName())
        assertEquals(4, player.amount(RAW_SHRIMP))

        CookingModule.stopAction(player)
        assertNull(player.activeActionName())

        player.advanceTicks(10)
        assertEquals(4, player.amount(RAW_SHRIMP))
    }

    @Test
    fun `running out of the raw item mid-batch stops with a message`() {
        val player = FakeSkillPlayer(mapOf(RAW_SHRIMP to 2)).apply { setLevel(Skill.COOKING, 99) }

        CookingModule.startCooking(player, RAW_SHRIMP, 5)
        player.advanceTicks(9)

        assertTrue(player.messages.contains("You are out of fish"))
        assertNull(player.activeActionName())
    }
}
