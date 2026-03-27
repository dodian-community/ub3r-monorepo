package net.dodian.uber.game.systems.combat

import net.dodian.uber.game.Server
import net.dodian.uber.game.systems.combat.style.CombatStyle
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Equipment

object UnarmedAttackAnimationService {
    private const val PUNCH_ANIMATION = 422
    private const val KICK_ANIMATION = 423

    @JvmStatic
    fun resolve(player: Client): Int {
        val weaponId = player.equipment[Equipment.Slot.WEAPON.id]
        if (weaponId > 0) {
            return Server.itemManager.getAttackAnim(weaponId)
        }
        return when (player.combatStyle) {
            CombatStyle.AGGRESSIVE_MELEE -> KICK_ANIMATION
            else -> PUNCH_ANIMATION
        }
    }
}
