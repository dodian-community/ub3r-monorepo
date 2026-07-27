package net.dodian.uber.skills.thieving

import net.dodian.uber.game.api.plugin.skills.SkillObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillObjectRef
import net.dodian.uber.game.api.plugin.skills.SkillPosition
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.testkit.FakeSkillPlayer
import net.dodian.uber.skills.testkit.LiveSkillModuleFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private const val SPECIAL_CHEST_OBJECT_ID = 375
private const val EMPTY_CHEST_OBJECT_ID = 378
private const val YANILLE_CHEST_X = 2593
private const val YANILLE_CHEST_Y = 3108
private const val FUR_STALL_OBJECT_ID = 11732

class ThievingModuleRuntimeTest {
    @Test
    fun `descriptor and manifest are wired consistently`() {
        LiveSkillModuleFixture.requirePlugin(ThievingModule.descriptor.id, Skill.THIEVING)
        assertEquals(ThievingModule.descriptor.id, ThievingModule.contentManifest.id)
    }

    @Test
    fun `toml targets are loaded`() {
        assertEquals(9, ThievingModule.targets.size)
        assertTrue(ThievingModule.targets.any { it.name == "farmer" })
        assertFalse(ThievingModule.targets.any { it.name == "man" })
    }

    @Test
    fun `cage_alt 20885 is registered as the baseline other target`() {
        assertTrue(ThievingModule.targets.any { it.id == 20885 && it.targetType == "other" })
    }

    // Regression test for the reported bug: clicking the Yanille chest produced no loot message,
    // no gfx, and the wrong replacement orientation because the shared replaceObject primitive
    // was client-local (dropped the restore timer) and never carried face/type.
    @Test
    fun `Yanille chest grants loot, plays gfx, yells on rare drop, and swaps to the empty chest with the correct orientation`() {
        val binding = ThievingModule.definition.objectBindings.single { it.option == 1 && SPECIAL_CHEST_OBJECT_ID in it.objectIds }
        val player = FakeSkillPlayer().apply { setLevel(Skill.THIEVING, 99) }
        val target = SkillObjectRef(SPECIAL_CHEST_OBJECT_ID, SkillPosition(YANILLE_CHEST_X, YANILLE_CHEST_Y, 1))

        val handled = binding.handler(SkillObjectInteraction(player, 1, target))

        assertTrue(handled)
        assertTrue(player.messages.any { it.startsWith("You find") || it.startsWith("You have recieved") })
        assertEquals(listOf(444 to target.position), player.graphicsPlayed)
        val replacement = player.replacedObjects.single()
        assertEquals(EMPTY_CHEST_OBJECT_ID, replacement.second)
        assertEquals(SPECIAL_CHEST_OBJECT_ID, replacement.first.id)
    }

    @Test
    fun `Yanille chest is a no-op off its plane`() {
        val binding = ThievingModule.definition.objectBindings.single { it.option == 1 && SPECIAL_CHEST_OBJECT_ID in it.objectIds }
        val player = FakeSkillPlayer().apply { setLevel(Skill.THIEVING, 99) }
        val wrongPlane = SkillObjectRef(SPECIAL_CHEST_OBJECT_ID, SkillPosition(YANILLE_CHEST_X, YANILLE_CHEST_Y, 0))

        val handled = binding.handler(SkillObjectInteraction(player, 1, wrongPlane))

        assertFalse(handled)
        assertTrue(player.messages.isEmpty())
    }

    @Test
    fun `stealing from a stall swaps to the empty stall id with the tile's face`() {
        val binding = ThievingModule.definition.objectBindings.single { it.option == 2 && FUR_STALL_OBJECT_ID in it.objectIds }
        val player = FakeSkillPlayer().apply { setLevel(Skill.THIEVING, 99) }
        // One of the legacy per-tile stall face lookup coordinates (expected face 0).
        val target = SkillObjectRef(FUR_STALL_OBJECT_ID, SkillPosition(2658, 3297, 0))

        val handled = binding.handler(SkillObjectInteraction(player, 2, target))

        assertTrue(handled)
        assertTrue(player.messages.any { it.startsWith("You receive") })
        assertEquals(634, player.replacedObjects.single().second)
    }
}
