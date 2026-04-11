package net.dodian.uber.game.api.content

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.content.combat.CombatLogoutLockService

object ContentSafety {
    @JvmStatic
    fun isLogoutLocked(player: Client): Boolean = CombatLogoutLockService.isLocked(player)

    @JvmStatic
    fun logoutLockRemainingSeconds(player: Client): Int = CombatLogoutLockService.remainingSeconds(player)
}
