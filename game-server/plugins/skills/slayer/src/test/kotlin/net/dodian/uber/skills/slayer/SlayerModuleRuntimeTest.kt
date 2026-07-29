package net.dodian.uber.skills.slayer

import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.api.plugin.skills.SkillEquipmentSlot
import net.dodian.uber.skills.testkit.FakeSkillPlayer
import net.dodian.uber.skills.testkit.LiveSkillModuleFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SlayerModuleRuntimeTest {
    @Test
    fun `descriptor and manifest are wired consistently`() {
        LiveSkillModuleFixture.requirePlugin(SlayerModule.descriptor.id, Skill.SLAYER)
        assertEquals(SlayerModule.descriptor.id, SlayerModule.contentManifest.id)
    }

    @Test
    fun `complete task registry preserves legacy ordinal ordering`() {
        assertEquals(34, SlayerModule.tasks.size)
        assertEquals("Crawling Hands", SlayerModule.tasks.first().name)
        assertEquals("Abyssal guardian", SlayerModule.tasks.last().name)
        assertEquals("Abyssal Demons", SlayerTaskRegistry.forOrdinal(20)?.name)
    }

    @Test
    fun `combat rules enforce active task and imbued mask bonus`() {
        val player = FakeSkillPlayer().apply {
            slayerTaskOrdinal = 20
            slayerAssignmentAmount = 30
            slayerRemainingAmount = 1
            setLevel(Skill.SLAYER, 99)
            equip(SkillEquipmentSlot.HEAD, 11784)
        }
        assertEquals(null, SlayerCombatService.canAttack(player, 415, false))
        assertEquals(2, SlayerCombatService.damageBonus(player, 415, true))
        assertEquals(2, SlayerCombatService.damageBonus(player, 415, false))
    }

    @Test
    fun `death completion applies legacy tenth-task bonus`() {
        val player = FakeSkillPlayer().apply {
            slayerTaskOrdinal = 20
            slayerAssignmentAmount = 30
            slayerRemainingAmount = 1
            slayerStreak = 9
        }
        val outcome = SlayerDeathService.onNpcDeath(player, 415, 100)!!
        assertTrue(outcome.completion)
        assertEquals(10, outcome.streak)
        assertEquals(1_100, outcome.baseXp)
        assertEquals(6_000, outcome.bonusXp)
    }
}
