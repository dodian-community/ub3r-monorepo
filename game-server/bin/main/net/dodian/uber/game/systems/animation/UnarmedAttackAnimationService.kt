package net.dodian.uber.game.systems.animation

import net.dodian.uber.game.Server
import net.dodian.uber.game.systems.combat.style.CombatStyle
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Equipment

/**
 * Resolves the melee attack animation for a player.
 *
 * Priority order:
 *  1. Equipped weapon's attackAnim from item definitions (covers combat weapons)
 *  2. Tool-weapon fallback — pickaxes and axes store attackAnim = 0 in the DB because
 *     they are loaded as skilling tools, not combat weapons. Without a fallback they would
 *     receive the generic defaultAttackAnim (806) which plays with no weapon swing and
 *     causes the held item to visually disappear during the hit frame.
 *  3. Unarmed punch / kick based on fight style
 */
object UnarmedAttackAnimationService {
    private const val PUNCH_ANIMATION = 422
    private const val KICK_ANIMATION = 423

    // Standard RS2 tool-weapon combat animations
    private const val PICKAXE_ATTACK_ANIMATION = 401
    private const val AXE_ATTACK_ANIMATION = 395

    // Sentinel returned by ItemManager when no valid attackAnim is configured
    private const val DEFAULT_ATTACK_ANIM = 806

    @JvmStatic
    fun resolve(player: Client): Int {
        val weaponId = player.equipment[Equipment.Slot.WEAPON.id]
        if (weaponId > 0) {
            val anim = Server.itemManager.getAttackAnim(weaponId)
            if (anim != DEFAULT_ATTACK_ANIM) {
                return anim
            }
            // attackAnim was not set for this item — check if it is a known tool type
            val name = Server.itemManager.getName(weaponId).lowercase()
            return when {
                name.contains("pickaxe") -> PICKAXE_ATTACK_ANIMATION
                // "axe" matches hatchets/axes but must not match "pickaxe" (already handled)
                // or "battleaxe" (those have real attack anims in the DB)
                name.contains("axe") && !name.contains("battle") -> AXE_ATTACK_ANIMATION
                else -> anim // fall back to whatever the DB returned (defaultAttackAnim)
            }
        }
        return when (player.combatStyle) {
            CombatStyle.AGGRESSIVE_MELEE -> KICK_ANIMATION
            else -> PUNCH_ANIMATION
        }
    }
}
