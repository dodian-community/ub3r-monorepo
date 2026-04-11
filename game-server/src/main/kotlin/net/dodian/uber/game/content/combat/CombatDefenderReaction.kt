package net.dodian.uber.game.content.combat

import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.animation.PlayerAnimationService
import net.dodian.uber.game.systems.animation.PlayerBlockAnimationService
import net.dodian.uber.game.engine.loop.GameCycleClock

object CombatDefenderReaction {
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
        PlayerAnimationService.requestBlockReaction(defender, animationId)
    }
}
