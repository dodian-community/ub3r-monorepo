package net.dodian.uber.skills.prayer

import net.dodian.uber.game.api.plugin.skills.SkillPlayer
import net.dodian.uber.game.api.plugin.skills.SkillPrayer

/**
 * Prayer-owned combat modifiers.  The engine supplies only the player's
 * active-prayer view; prayer rules remain with the Prayer skill module.
 */
object PrayerCombatService {
    fun disableProtectionPrayers(player: SkillPlayer) {
        PROTECTION_PRAYERS.forEach(player.vitals::deactivatePrayer)
    }
    /** Legacy prayer-level contribution to the damage-neglect proc chance. */
    @JvmStatic fun damageNeglectChance(prayerLevel: Int): Double = ((prayerLevel + 1) / 8.0) / 100.0 + 0.025

    fun meleeAttackMultiplier(player: SkillPlayer): Double = when {
        player.vitals.isPrayerActive(SkillPrayer.CLARITY_OF_THOUGHT) -> 1.05
        player.vitals.isPrayerActive(SkillPrayer.IMPROVED_REFLEXES) -> 1.10
        player.vitals.isPrayerActive(SkillPrayer.INCREDIBLE_REFLEXES) -> 1.15
        player.vitals.isPrayerActive(SkillPrayer.CHIVALRY) -> 1.18
        player.vitals.isPrayerActive(SkillPrayer.PIETY) -> 1.22
        else -> 1.0
    }

    fun rangedAttackMultiplier(player: SkillPlayer): Double = when {
        player.vitals.isPrayerActive(SkillPrayer.SHARP_EYE) -> 1.025
        player.vitals.isPrayerActive(SkillPrayer.HAWK_EYE) -> 1.05
        player.vitals.isPrayerActive(SkillPrayer.EAGLE_EYE) -> 1.075
        else -> 1.0
    }

    fun defenceMultiplier(player: SkillPlayer): Double = when {
        player.vitals.isPrayerActive(SkillPrayer.THICK_SKIN) -> 1.05
        player.vitals.isPrayerActive(SkillPrayer.ROCK_SKIN) -> 1.10
        player.vitals.isPrayerActive(SkillPrayer.STEEL_SKIN) -> 1.15
        player.vitals.isPrayerActive(SkillPrayer.CHIVALRY) -> 1.18
        player.vitals.isPrayerActive(SkillPrayer.PIETY) -> 1.22
        else -> 1.0
    }

    @JvmStatic fun protectsFromMelee(player: SkillPlayer): Boolean =
        player.vitals.isPrayerActive(SkillPrayer.PROTECT_MELEE)
    @JvmStatic fun protectsFromRange(player: SkillPlayer): Boolean =
        player.vitals.isPrayerActive(SkillPrayer.PROTECT_RANGE)
    @JvmStatic fun protectsFromMagic(player: SkillPlayer): Boolean =
        player.vitals.isPrayerActive(SkillPrayer.PROTECT_MAGIC)

    private val PROTECTION_PRAYERS = listOf(SkillPrayer.PROTECT_MELEE, SkillPrayer.PROTECT_RANGE, SkillPrayer.PROTECT_MAGIC)

    fun magicDamageBonus(player: SkillPlayer): Double = when {
        player.vitals.isPrayerActive(SkillPrayer.MYSTIC_WILL) -> 0.05
        player.vitals.isPrayerActive(SkillPrayer.MYSTIC_LORE) -> 0.10
        player.vitals.isPrayerActive(SkillPrayer.MYSTIC_MIGHT) -> 0.15
        else -> 0.0
    }

    fun meleeStrengthBonus(player: SkillPlayer): Double = when {
        player.vitals.isPrayerActive(SkillPrayer.BURST_OF_STRENGTH) -> 0.05
        player.vitals.isPrayerActive(SkillPrayer.SUPERHUMAN_STRENGTH) -> 0.10
        player.vitals.isPrayerActive(SkillPrayer.ULTIMATE_STRENGTH) -> 0.15
        player.vitals.isPrayerActive(SkillPrayer.CHIVALRY) -> 0.18
        player.vitals.isPrayerActive(SkillPrayer.PIETY) -> 0.22
        else -> 0.0
    }

    fun rangedStrengthBonus(player: SkillPlayer): Double = when {
        player.vitals.isPrayerActive(SkillPrayer.SHARP_EYE) -> 0.05
        player.vitals.isPrayerActive(SkillPrayer.HAWK_EYE) -> 0.10
        player.vitals.isPrayerActive(SkillPrayer.EAGLE_EYE) -> 0.15
        else -> 0.0
    }
}
