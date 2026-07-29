package net.dodian.uber.skills.runecrafting

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

private const val NATURE_ALTAR_OBJECT_ID = 34768
private const val RUNE_ESSENCE = 1436

private fun altarRef(id: Int = NATURE_ALTAR_OBJECT_ID) = SkillObjectRef(id, SkillPosition(3200, 3200, 0))

class RunecraftingModuleRuntimeTest {
    @Test
    fun `descriptor and manifest are wired consistently`() {
        LiveSkillModuleFixture.requirePlugin(RunecraftingModule.descriptor.id, Skill.RUNECRAFTING)
        assertEquals(RunecraftingModule.descriptor.id, RunecraftingModule.contentManifest.id)
    }

    @Test
    fun `data loads all 3 altars`() {
        assertEquals(3, RunecraftingModule.altars.size)
        assertTrue(RunecraftingModule.altars.any { it.objectId == NATURE_ALTAR_OBJECT_ID })
        assertTrue(RunecraftingModule.altars.any { it.objectId == 34766 })
        assertFalse(RunecraftingModule.altars.any { 14905 in it.objectIds || 14903 in it.objectIds })
    }

    @Test
    fun `crafting at an altar consumes essence and grants runes and xp`() {
        val altar = RunecraftingModule.altars.single { it.objectId == NATURE_ALTAR_OBJECT_ID }
        val binding = RunecraftingModule.definition.objectBindings.single { NATURE_ALTAR_OBJECT_ID in it.objectIds }
        val player = FakeSkillPlayer(mapOf(RUNE_ESSENCE to 5)).apply { setLevel(Skill.RUNECRAFTING, 1) }

        val handled = binding.handler(SkillObjectInteraction(player, 1, altarRef()))

        assertTrue(handled)
        assertEquals(0, player.amount(RUNE_ESSENCE))
        assertTrue(player.amount(altar.runeId) >= 5, "expected at least one rune per essence")
        assertEquals(altar.experiencePerEssence * 5, player.skills.experience(Skill.RUNECRAFTING))
        assertTrue(player.messages.single().startsWith("You craft"))
    }

    @Test
    fun `no essence refuses to craft`() {
        val binding = RunecraftingModule.definition.objectBindings.single { NATURE_ALTAR_OBJECT_ID in it.objectIds }
        val player = FakeSkillPlayer().apply { setLevel(Skill.RUNECRAFTING, 1) }

        val handled = binding.handler(SkillObjectInteraction(player, 1, altarRef()))

        assertFalse(handled)
        assertEquals(listOf("You do not have any rune essence!"), player.messages)
    }

    @Test
    fun `insufficient level refuses to craft at the higher altar`() {
        val lawAltar = RunecraftingModule.altars.single { it.requiredLevel == 50 }
        val binding = RunecraftingModule.definition.objectBindings.single { lawAltar.objectId in it.objectIds }
        val player = FakeSkillPlayer(mapOf(RUNE_ESSENCE to 5)).apply { setLevel(Skill.RUNECRAFTING, 1) }

        val handled = binding.handler(SkillObjectInteraction(player, 1, altarRef(lawAltar.objectId)))

        assertFalse(handled)
        assertEquals(5, player.amount(RUNE_ESSENCE))
        assertEquals(0, player.skills.experience(Skill.RUNECRAFTING))
    }

    @Test
    fun `records last altar craft timestamp as a plugin attribute`() {
        val binding = RunecraftingModule.definition.objectBindings.single { NATURE_ALTAR_OBJECT_ID in it.objectIds }
        val player = FakeSkillPlayer(mapOf(RUNE_ESSENCE to 1)).apply { setLevel(Skill.RUNECRAFTING, 1) }

        binding.handler(SkillObjectInteraction(player, 1, altarRef()))

        assertTrue((player.attributes.get(RunecraftingModule.LAST_ALTAR_CRAFT_KEY) ?: 0L) > 0L)
    }

    @Test
    fun `pouch fill and empty preserve essence through the plugin service`() {
        val player = FakeSkillPlayer(mapOf(RUNE_ESSENCE to 4)).apply { setLevel(Skill.RUNECRAFTING, 99) }

        assertTrue(RunecraftingPouchService.fill(player, 5509))
        assertEquals(0, player.amount(RUNE_ESSENCE))
        assertEquals(4, player.runePouches.amount(0))
        assertTrue(RunecraftingPouchService.empty(player, 5509))
        assertEquals(4, player.amount(RUNE_ESSENCE))
        assertEquals(0, player.runePouches.amount(0))
    }

    @Test
    fun `pouch inspect and level rejection are handled by the plugin`() {
        val lowLevel = FakeSkillPlayer()

        assertTrue(RunecraftingPouchService.fill(lowLevel, 5511))
        assertEquals(listOf("You need level 20 runecrafting to do this!"), lowLevel.messages)
        assertTrue(RunecraftingPouchService.check(lowLevel, 5509))
        assertEquals("There is 0 rune essence in this pouch!", lowLevel.messages.last())
    }
}
