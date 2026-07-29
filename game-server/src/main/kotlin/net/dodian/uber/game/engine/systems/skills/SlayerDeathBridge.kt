package net.dodian.uber.game.engine.systems.skills

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.skills.slayer.SlayerDeathService

object SlayerDeathBridge {
    @JvmStatic fun onNpcDeath(player: Client, npcId: Int, maxHealth: Int) {
        val outcome = SlayerDeathService.onNpcDeath(player.asSkillPlayer(), npcId, maxHealth) ?: return
        player.slayerTaskState = player.slayerTaskState.withRemainingAmount(outcome.remaining).withStreak(outcome.streak)
        ProgressionService.addXp(player, outcome.baseXp, Skill.SLAYER)
        SkillingRandomEventService.trigger(player, outcome.baseXp)
        if (outcome.completion) player.send(SendMessage("<col=FF8C00>You have completed your slayer task! <col=FF0000>|</col> Current streak is ${outcome.streak}."))
        if (outcome.bonusXp > 0) {
            ProgressionService.addXp(player, outcome.bonusXp, Skill.SLAYER)
            player.send(SendMessage("<col=FF8C00>You have gained some bonus experience from finishing your ${outcome.bonusMilestone} task in a row."))
        }
    }
}
