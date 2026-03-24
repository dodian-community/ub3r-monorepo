package net.dodian.uber.game.model.player.skills

object Skills {
    @JvmStatic
    fun getLevelForExperience(exp: Int): Int {
        var output = 0.0
        var playerLevel = 0
        var lvl = 2
        while (lvl <= 100 && output.toInt() <= exp) {
            output += kotlin.math.floor((lvl - 1) + 300 * Math.pow(2.0, (lvl - 1).toDouble() / 7.0)) / 4.0
            playerLevel++
            lvl++
        }
        return playerLevel
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
