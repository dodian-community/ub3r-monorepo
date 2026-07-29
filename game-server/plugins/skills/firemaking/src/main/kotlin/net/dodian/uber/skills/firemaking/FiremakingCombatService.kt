package net.dodian.uber.skills.firemaking

/** Firemaking-derived combat utility bonuses. */
object FiremakingCombatService {
    @JvmStatic fun dragonfireNeglectBonus(firemakingLevel: Int): Int = ((firemakingLevel + 1) / 5) * 10
}
