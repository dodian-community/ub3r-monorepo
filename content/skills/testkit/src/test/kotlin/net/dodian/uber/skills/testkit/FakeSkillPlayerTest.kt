package net.dodian.uber.skills.testkit

import net.dodian.uber.game.api.content.ContentAttributeKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import net.dodian.uber.game.api.plugin.skills.startProduction
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.api.SkillMultiConfig
import net.dodian.uber.skills.api.SkillMultiEntry
import net.dodian.uber.skills.api.SkillMultiSelection
import net.dodian.uber.skills.api.skillRecipe

class FakeSkillPlayerTest {

    @Test
    fun `amount prompt records and completes a module callback once`() {
        val player = FakeSkillPlayer()
        var submitted = 0

        assertTrue(player.amountPrompt.request("How many?") { amount -> submitted = amount; true })
        assertEquals("How many?", player.amountPromptTitle)
        assertTrue(player.submitAmount(12))
        assertEquals(12, submitted)
        assertNull(player.amountPromptTitle)
        assertFalse(player.submitAmount(1))
    }
    @Test
    fun `plugin attributes are available to isolated module tests`() {
        val player = FakeSkillPlayer()
        val selection = ContentAttributeKey<Int>("fletching", "selection")

        player.attributes.put(selection, 2)
        assertEquals(2, player.attributes.get(selection))

        player.attributes.remove(selection)
        assertNull(player.attributes.get(selection))
    }

    @Test
    fun `failed transaction leaves inventory unchanged and does not refresh`() {
        val player = FakeSkillPlayer(mapOf(100 to 1))
        val committed = player.inventory.transaction { remove(100, 2); add(200) }
        assertFalse(committed)
        assertEquals(1, player.amount(100))
        assertEquals(0, player.amount(200))
        assertEquals(0, player.refreshCount)
    }

    @Test
    fun `production menu clamps selection and completes transactional cycles`() {
        val player = FakeSkillPlayer(mapOf(317 to 3)).apply { setLevel(Skill.COOKING, 99) }
        val recipe = skillRecipe("cooking.shrimp", 315) {
            material(317)
            requirement(1)
            experience(30)
            animation(896)
            delay(2)
            success("You cook the shrimp.")
        }
        val config = SkillMultiConfig("cooking.range", "cook", entries = listOf(SkillMultiEntry(recipe)))
        player.production.open(config) { selection -> player.startProduction(recipe, selection.amount, Skill.COOKING) }

        val selected = player.production.select(SkillMultiSelection(config.key, recipe.key, 20))
        assertEquals(true, selected)
        assertEquals("production.cooking.shrimp", player.activeActionName())

        player.advanceTicks(5)
        assertEquals(0, player.amount(317))
        assertEquals(3, player.amount(315))
        assertEquals(90, player.skills.experience(Skill.COOKING))
        assertEquals(3, player.messages.count { it == "You cook the shrimp." })
        assertEquals(3, player.refreshCount)
    }

    @Test
    fun `material loss between cycles stops without output xp or success message`() {
        val player = FakeSkillPlayer(mapOf(100 to 2)).apply { setLevel(Skill.CRAFTING, 99) }
        val recipe = skillRecipe("crafting.test", 200) {
            material(100)
            experience(10)
            delay(2)
            success("made")
        }
        player.startProduction(recipe, 2, Skill.CRAFTING)
        player.advanceTicks()
        assertEquals(true, player.inventory.remove(100, 1))
        player.advanceTicks(2)

        assertEquals(1, player.amount(200))
        assertEquals(10, player.skills.experience(Skill.CRAFTING))
        assertEquals(1, player.messages.count { it == "made" })
        assertEquals(null, player.activeActionName())
    }
}
