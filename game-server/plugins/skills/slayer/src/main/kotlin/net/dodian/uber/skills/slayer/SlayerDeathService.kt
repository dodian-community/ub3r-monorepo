package net.dodian.uber.skills.slayer

import net.dodian.uber.game.api.plugin.skills.SkillPlayer

data class SlayerDeathOutcome(val remaining: Int, val streak: Int, val baseXp: Int, val bonusXp: Int, val completion: Boolean, val bonusMilestone: Int?)

/** Computes task-kill progression; the engine bridge applies persistence and packets. */
object SlayerDeathService {
    fun onNpcDeath(player: SkillPlayer, npcId: Int, maxHealth: Int): SlayerDeathOutcome? {
        val task = SlayerTaskRegistry.forNpc(npcId) ?: return null
        if (player.slayerTask.ordinal != SlayerTaskRegistry.tasks.indexOf(task) || player.slayerTask.remainingAmount <= 0) return null
        val remaining = player.slayerTask.remainingAmount - 1
        val baseXp = maxHealth * 11
        if (remaining > 0) return SlayerDeathOutcome(remaining, player.slayerTask.streak, baseXp, 0, false, null)
        val streak = player.slayerTask.streak + 1
        val milestone = intArrayOf(1000, 500, 250, 100, 50, 10).firstOrNull { streak % it == 0 }
        val bonusXp = milestone?.let { m -> intArrayOf(50, 30, 20, 11, 6, 2)[intArrayOf(1000,500,250,100,50,10).indexOf(m)] * player.slayerTask.assignmentAmount * maxHealth } ?: 0
        return SlayerDeathOutcome(remaining, streak, baseXp, bonusXp, true, milestone)
    }
}
