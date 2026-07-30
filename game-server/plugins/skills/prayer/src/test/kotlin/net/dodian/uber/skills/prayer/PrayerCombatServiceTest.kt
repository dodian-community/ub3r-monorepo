package net.dodian.uber.skills.prayer

import net.dodian.uber.game.api.plugin.skills.SkillPrayer
import net.dodian.uber.skills.testkit.FakeSkillPlayer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PrayerCombatServiceTest {
    @Test
    fun `keeps legacy prayer damage-neglect chance`() {
        assertEquals(0.15, PrayerCombatService.damageNeglectChance(99))
    }

    @Test
    fun `uses the active strength prayer for melee damage`() {
        val player = FakeSkillPlayer().apply { activePrayers += SkillPrayer.PIETY }

        assertEquals(0.22, PrayerCombatService.meleeStrengthBonus(player))
    }

    @Test
    fun `uses ranged prayer for accuracy and strength independently`() {
        val player = FakeSkillPlayer().apply { activePrayers += SkillPrayer.EAGLE_EYE }

        assertEquals(1.075, PrayerCombatService.rangedAttackMultiplier(player))
        assertEquals(0.15, PrayerCombatService.rangedStrengthBonus(player))
    }

    @Test
    fun `reports protection and defence modifiers from the public prayer state`() {
        val player = FakeSkillPlayer().apply {
            activePrayers += SkillPrayer.PROTECT_MELEE
            activePrayers += SkillPrayer.STEEL_SKIN
        }

        assertTrue(PrayerCombatService.protectsFromMelee(player))
        assertEquals(1.15, PrayerCombatService.defenceMultiplier(player))
        assertFalse(PrayerCombatService.protectsFromMelee(FakeSkillPlayer()))
    }

    @Test
    fun `range and magic protection are independent of melee protection`() {
        val rangeProtected = FakeSkillPlayer().apply { activePrayers += SkillPrayer.PROTECT_RANGE }
        val magicProtected = FakeSkillPlayer().apply { activePrayers += SkillPrayer.PROTECT_MAGIC }

        assertTrue(PrayerCombatService.protectsFromRange(rangeProtected))
        assertFalse(PrayerCombatService.protectsFromMagic(rangeProtected))
        assertTrue(PrayerCombatService.protectsFromMagic(magicProtected))
        assertFalse(PrayerCombatService.protectsFromRange(magicProtected))
    }
}
