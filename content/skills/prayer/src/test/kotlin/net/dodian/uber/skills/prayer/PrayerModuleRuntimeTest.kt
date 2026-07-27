package net.dodian.uber.skills.prayer

import net.dodian.uber.game.api.plugin.skills.SkillItemInteraction
import net.dodian.uber.game.api.plugin.skills.SkillObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillObjectRef
import net.dodian.uber.game.api.plugin.skills.SkillPosition
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.testkit.FakeSkillPlayer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PrayerModuleRuntimeTest {
    @Test
    fun `burying a bone consumes it and grants xp`() {
        val player = FakeSkillPlayer(mapOf(526 to 1))
        val binding = PrayerModule.definition.itemBindings.single { 526 in it.itemIds }
        assertTrue(binding.handler(SkillItemInteraction(player, 1, 526, 0, -1)))
        assertEquals(0, player.amount(526))
        assertEquals(45, player.skills.experience(Skill.PRAYER))
    }

    @Test
    fun `altar restores prayer through the public vitals contract`() {
        val player = FakeSkillPlayer().apply { currentPrayerValue = 10; maximumPrayerValue = 50 }
        val binding = PrayerModule.definition.objectBindings.single { 409 in it.objectIds }
        assertTrue(binding.handler(SkillObjectInteraction(player, 1, SkillObjectRef(409, SkillPosition(3200, 3200)))))
        assertEquals(50, player.currentPrayerValue)
    }
}
