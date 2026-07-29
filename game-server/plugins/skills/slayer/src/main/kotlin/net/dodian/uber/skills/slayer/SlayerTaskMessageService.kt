package net.dodian.uber.skills.slayer

import net.dodian.uber.game.api.plugin.skills.SkillPlayer

object SlayerTaskMessageService {
    fun sendCurrentTask(player: SkillPlayer) {
        val task = SlayerTaskRegistry.forOrdinal(player.slayerTask.ordinal)
        if (task != null && player.slayerTask.remainingAmount > 0) {
            player.ui.message("You need to kill ${player.slayerTask.remainingAmount} more of ${task.name} <col=FF0000>|</col> Current streak is ${player.slayerTask.streak}.")
        } else player.ui.message("You need to be assigned a task!")
    }
}
