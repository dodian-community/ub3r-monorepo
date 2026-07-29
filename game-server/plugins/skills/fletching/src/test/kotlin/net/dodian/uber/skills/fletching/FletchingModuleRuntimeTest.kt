package net.dodian.uber.skills.fletching

import net.dodian.uber.game.api.plugin.skills.SkillItemOnItemInteraction
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.api.SkillMultiSelection
import net.dodian.uber.skills.testkit.FakeSkillPlayer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FletchingModuleRuntimeTest {
    @Test
    fun `plugin registry owns bow and ammunition metadata`() {
        assertEquals(5, FletchingModule.bowLogs.size)
        assertTrue(FletchingModule.arrowRecipes.any { it.materialId == 11237 && it.productId == 11212 })
        assertTrue(FletchingModule.dartRecipes.any { it.materialId == 819 && it.productId == 806 })
    }

    @Test
    fun `knife on logs opens and executes the live transactional route`() {
        assertEquals(FletchingModule.descriptor.id, FletchingModule.contentManifest.id)
        val binding = FletchingModule.definition.itemOnItemBindings.single { it.leftItemId == 946 && it.rightItemId == 1521 }
        val player = FakeSkillPlayer(mapOf(946 to 1, 1521 to 2)).apply { setLevel(Skill.FLETCHING, 99) }

        assertTrue(binding.handler(SkillItemOnItemInteraction(player, 946, 1521)))
        val menu = player.production.pending()!!
        val shortbow = menu.entries.first()
        assertTrue(player.production.select(SkillMultiSelection(menu.key, shortbow.recipe.key, 2)))
        player.advanceTicks(8)

        assertEquals(0, player.amount(1521))
        assertEquals(2, player.amount(54))
        assertEquals(204, player.skills.experience(Skill.FLETCHING))
        assertEquals(2, player.messages.size)
        assertEquals(null, player.activeActionName())
    }

    @Test
    fun `ammo and stringing routes are plugin owned and transactional`() {
        val dart = FletchingModule.definition.itemOnItemBindings.first { setOf(it.leftItemId, it.rightItemId) == setOf(314, 819) }
        val player = FakeSkillPlayer(mapOf(314 to 2, 819 to 2)).apply { setLevel(Skill.FLETCHING, 99) }
        assertTrue(dart.handler(SkillItemOnItemInteraction(player, 314, 819)))
        val menu = player.production.pending()!!
        assertTrue(player.production.select(SkillMultiSelection(menu.key, menu.entries.single().recipe.key, 2)))
        player.advanceTicks(8)
        assertEquals(20, player.amount(806))
        assertEquals(0, player.amount(314))
        assertEquals(0, player.amount(819))
    }
}
