package net.dodian.uber.skills.herblore

import net.dodian.uber.game.api.plugin.skills.SkillItemInteraction
import net.dodian.uber.game.api.plugin.skills.SkillItemOnItemInteraction
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.api.SkillMultiSelection
import net.dodian.uber.skills.testkit.FakeSkillPlayer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HerbloreModuleRuntimeTest {
    @Test
    fun `grimy herbs and potion recipes are plugin owned`() {
        assertEquals(HerbloreModule.descriptor.id, HerbloreModule.contentManifest.id)
        val player = FakeSkillPlayer(mapOf(199 to 1, 249 to 1, 227 to 1)).apply { setLevel(Skill.HERBLORE, 99) }
        val clean = HerbloreModule.definition.itemBindings.single { 199 in it.itemIds }
        assertTrue(clean.handler(SkillItemInteraction(player, 1, 199, 0, -1)))
        assertEquals(2, player.amount(249))

        val mix = HerbloreModule.definition.itemOnItemBindings.first { setOf(it.leftItemId, it.rightItemId) == setOf(249, 227) }
        assertTrue(mix.handler(SkillItemOnItemInteraction(player, 249, 227)))
        val menu = player.production.pending()!!
        assertTrue(player.production.select(SkillMultiSelection(menu.key, menu.entries.single().recipe.key, 1)))
        player.advanceTicks(2)
        assertEquals(1, player.amount(91))
    }

    @Test
    fun `premium recipes refuse non premium players`() {
        val player = FakeSkillPlayer(mapOf(259 to 1, 227 to 1)).apply { setLevel(Skill.HERBLORE, 99) }
        val binding = HerbloreModule.definition.itemOnItemBindings.first { setOf(it.leftItemId, it.rightItemId) == setOf(259, 227) }
        assertTrue(binding.handler(SkillItemOnItemInteraction(player, 259, 227)))
        val menu = player.production.pending()!!
        assertTrue(player.production.select(SkillMultiSelection(menu.key, menu.entries.single().recipe.key, 1)))
        player.advanceTicks(1)
        assertEquals(1, player.amount(259))
        assertEquals(1, player.amount(227))
    }
}
