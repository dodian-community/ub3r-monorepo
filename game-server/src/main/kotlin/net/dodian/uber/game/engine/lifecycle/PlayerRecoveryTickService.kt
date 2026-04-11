package net.dodian.uber.game.engine.lifecycle

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.Skills
import net.dodian.uber.game.persistence.player.PlayerSaveSegment
import net.dodian.uber.game.engine.systems.skills.ProgressionService

object PlayerRecoveryTickService {
    private const val RECOVERY_EFFECT_TICKS = 25
    private const val RECOVERY_STAGE_TICKS = 4

    @JvmStatic
    fun process(player: Client) {
        player.lastRecoverEffect++
        if (player.lastRecoverEffect % RECOVERY_EFFECT_TICKS != 0) {
            return
        }

        player.lastRecover--
        if (player.lastRecover != 0) {
            return
        }

        player.lastRecoverEffect = 0
        player.lastRecover = RECOVERY_STAGE_TICKS

        Skill.enabledSkills().forEach { skill ->
            var changed = false
            when (skill) {
                Skill.HITPOINTS -> {
                    if (player.currentHealth < player.maxHealth) {
                        player.heal(1)
                        changed = true
                    }
                }

                Skill.PRAYER -> Unit

                else -> {
                    val skillId = skill.getId()
                    val before = player.boostedLevel[skillId]
                    if (before > 0) {
                        player.boostedLevel[skillId]--
                    } else if (before != 0) {
                        player.boostedLevel[skillId]++
                    }
                    changed = player.boostedLevel[skillId] != before
                }
            }

            if (changed) {
                ProgressionService.refresh(player, skill)
                player.markSaveDirty(PlayerSaveSegment.STATS.mask)
            }
        }
    }
}
