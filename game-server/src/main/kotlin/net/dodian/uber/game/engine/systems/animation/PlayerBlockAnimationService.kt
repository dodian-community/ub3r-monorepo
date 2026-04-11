package net.dodian.uber.game.engine.systems.animation

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Equipment

object PlayerBlockAnimationService {
    private const val UNARMED_BLOCK_ANIMATION = 404
    private const val SHIELD_BLOCK_ANIMATION = 1156
    private const val TWO_HANDED_BLOCK_ANIMATION = 410
    private const val STAFF_BLOCK_ANIMATION = 420
    private const val DAGGER_BLOCK_ANIMATION = 378
    private const val BOW_BLOCK_ANIMATION = 424
    private const val ARMED_BLOCK_ANIMATION = 424

    @JvmStatic
    fun resolve(player: Client): Int {
        val weapon = player.equipment[Equipment.Slot.WEAPON.id]
        val shield = player.equipment[Equipment.Slot.SHIELD.id]
        if (shield > 0) {
            return SHIELD_BLOCK_ANIMATION
        }
        if (weapon <= 0) {
            return UNARMED_BLOCK_ANIMATION
        }
        val weaponName = Server.itemManager.getName(weapon).lowercase()
        return when {
            Server.itemManager.isTwoHanded(weapon) ||
                weaponName.contains("2h sword") ||
                weaponName.contains("halberd") ||
                weaponName.contains("maul") ||
                weaponName.contains("godsword") -> TWO_HANDED_BLOCK_ANIMATION
            weaponName.contains("staff") -> STAFF_BLOCK_ANIMATION
            weaponName.contains("dagger") -> DAGGER_BLOCK_ANIMATION
            weaponName.contains("bow") -> BOW_BLOCK_ANIMATION
            else -> ARMED_BLOCK_ANIMATION
        }
    }
}

