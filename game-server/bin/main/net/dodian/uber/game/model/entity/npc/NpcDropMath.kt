package net.dodian.uber.game.model.entity.npc

import net.dodian.utilities.Misc

object NpcDropMath {
    @JvmStatic
    fun shouldDrop(percent: Double, wealth: Boolean): Boolean {
        val adjustedChance =
            if (wealth && percent <= 1.0) {
                percent * 1.2
            } else if (percent < 10.0) {
                percent * 1.1
            } else {
                percent
            }
        return (Math.random() * 100) <= adjustedChance
    }

    @JvmStatic
    fun rollAmount(minAmount: Int, maxAmount: Int): Int {
        return if (minAmount == maxAmount) minAmount else minAmount + Misc.random(maxAmount - minAmount)
    }
}

