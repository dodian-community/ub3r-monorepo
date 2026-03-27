package net.dodian.uber.game.runtime.combat

import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.animation.PlayerAnimationService
import net.dodian.uber.game.runtime.loop.GameCycleClock
import net.dodian.utilities.combatReactionDebugEnabled
import org.slf4j.LoggerFactory

object CombatDefenderReaction {
    private val logger = LoggerFactory.getLogger(CombatDefenderReaction::class.java)

    @JvmStatic
    fun playBlockAnimation(
        defender: Client,
        damage: Int,
        damageType: Entity.damageType?,
    ) {
        if (damage < 0) {
            return
        }
        if (damageType == null) {
            return
        }
        if (damageType == Entity.damageType.MAGIC ||
            damageType == Entity.damageType.JAD_MAGIC ||
            damageType == Entity.damageType.FIRE_BREATH
        ) {
            return
        }

        val cycle = GameCycleClock.currentCycle()
        if (defender.lastBlockAnimationCycle >= cycle) {
            return
        }

        val animationId = PlayerBlockAnimationService.resolve(defender)
        defender.lastBlockAnimationCycle = cycle
        if (combatReactionDebugEnabled) {
            logger.info(
                "combat-reaction defender={} slot={} damage={} type={} anim={} cycle={}",
                defender.playerName,
                defender.slot,
                damage,
                damageType,
                animationId,
                cycle,
            )
        }
        PlayerAnimationService.requestBlockReaction(defender, animationId)
    }
}
