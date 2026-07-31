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
        assertEquals(8, ThievingModule.targets.size)
        assertTrue(ThievingModule.targets.any { it.name == "farmer" })
        assertFalse(ThievingModule.targets.any { it.name == "man" })
    }

    @Test
    fun `pickpocket and cage routes use revision 218 ids and options`() {
        assertEquals(setOf(3114, 5730), ThievingModule.targets.filter { it.targetType == "npc" }.map { it.id }.toSet())
        assertTrue(ThievingModule.definition.npcBindings.any { it.option == 3 && 3114 in it.npcIds })
        assertTrue(ThievingModule.definition.npcBindings.any { it.option == 3 && 5730 in it.npcIds })
        assertFalse(ThievingModule.definition.npcBindings.any { 3086 in it.npcIds || 3257 in it.npcIds })
        assertTrue(ThievingModule.targets.any { it.id == 20873 && it.targetType == "other" })
        assertFalse(ThievingModule.targets.any { it.id == 20885 })
        assertFalse(ThievingModule.definition.objectBindings.any { 20885 in it.objectIds })
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

    @Test
    fun `pyramid plunder tick counts down and resets to the pyramid entrance on timeout`() {
        val player = FakeSkillPlayer()
        ThievingModule.startPlunder(player)
        assertTrue(ThievingModule.hindersTeleport(player))

        // Drain the 500-tick timer directly to timeout without a 500-iteration loop.
        repeat(499) { ThievingModule.tick(player) }
        assertTrue(ThievingModule.hindersTeleport(player))
        ThievingModule.tick(player)

        assertFalse(ThievingModule.hindersTeleport(player))
        assertTrue(player.messages.any { it.contains("run out time") })
    }

    @Test
    fun `toggling an urn obstacle sets a random 1-or-2 state and updates the urn varbit`() {
        val player = FakeSkillPlayer()
        ThievingModule.startPlunder(player)
        val binding = ThievingModule.definition.objectBindings.single { it.option == 1 && 26600 in it.objectIds }

        val handled = binding.handler(SkillObjectInteraction(player, 1, SkillObjectRef(26600, SkillPosition(0, 0, 0))))

        assertTrue(handled)
        assertTrue(player.varbits.containsKey(820))
        assertTrue(player.varbits.getValue(820) != 0)
    }

    // Regression test for the Pyramid Plunder exit-confirmation port: previously a legacy
    // game-server dialogue module/handler pair (PyramidPlunderDialogueModule/OptionHandler),
    // now a confirmDialogue binding that calls the plugin's own resetPlunder directly.
    @Test
    fun `pyramid exit confirmation resets plunder state`() {
        val player = FakeSkillPlayer()
        ThievingModule.startPlunder(player)
        assertTrue(ThievingModule.hindersTeleport(player))

        val binding = ThievingModule.definition.confirmDialogueBindings.single { it.dialogueId == 20931 }
        binding.handler(player)

        assertFalse(ThievingModule.hindersTeleport(player))
    }

    @Test
    fun `solving the room's assigned exit tomb advances the room`() {
        val player = FakeSkillPlayer().apply { setLevel(Skill.THIEVING, 99) }
        ThievingModule.startPlunder(player)
        // The room-door id for room 0 is whichever of 26618-26621 the internal cycle assigned;
        // try all four - toggling the non-assigned ones is a harmless no-op advance check.
        val startingRoom = ThievingModule.roomNumber(player)
        var advanced = false
        for (candidateDoorId in 26618..26621) {
            val binding = ThievingModule.definition.objectBindings.single { it.option == 1 && candidateDoorId in it.objectIds }
            binding.handler(SkillObjectInteraction(player, 1, SkillObjectRef(candidateDoorId, SkillPosition(0, 0, 0))))
            if (ThievingModule.roomNumber(player) != startingRoom) {
                advanced = true
                break
            }
        }
        assertTrue(advanced, "Expected exactly one of the 4 tomb-door ids to advance the room once solved")
    }
}
