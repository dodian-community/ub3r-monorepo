package net.dodian.uber.game.model.player.skills

object Skills {
    @JvmStatic
    fun getLevelForExperience(exp: Int): Int {
        val safeExp = exp.coerceAtLeast(0)
        var points = 0.0
        for (level in 1..99) {
            points += kotlin.math.floor(level + 300.0 * Math.pow(2.0, level.toDouble() / 7.0))
            val xpForLevel = kotlin.math.floor(points / 4).toInt()
            if (safeExp < xpForLevel) {
                return level
            }
        }
        return 99
    }

    @JvmStatic
    fun getXPForLevel(level: Int): Int {
        var points = 0.0
        var output = 0
        for (lvl in 1 until level) {
            points += kotlin.math.floor(lvl + 300.0 * Math.pow(2.0, lvl.toDouble() / 7.0))
            output = kotlin.math.floor(points / 4).toInt()
        }
        return output
    }

    @JvmStatic
    fun maxTotalLevel(): Int {
        var enabledCount = 0
        var disabledCount = 0
        for (skill in Skill.values()) {
            if (skill.isEnabled()) enabledCount++ else disabledCount++
        }
        return (enabledCount * 99) + disabledCount
    }
}
