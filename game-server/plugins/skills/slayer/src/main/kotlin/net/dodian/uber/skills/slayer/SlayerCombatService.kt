package net.dodian.uber.skills.slayer

import net.dodian.uber.game.api.plugin.skills.SkillEquipmentSlot
import net.dodian.uber.game.api.plugin.skills.SkillPlayer
import net.dodian.uber.game.model.player.skills.Skill

/** Slayer task admission and black-mask combat effects. */
object SlayerCombatService {
    fun canAttack(player: SkillPlayer, npcId: Int, mummyAreaException: Boolean): String? {
        val task = SlayerTaskRegistry.forNpc(npcId)
        if (task != null && task.slayerOnly && !mummyAreaException &&
            (player.slayerTask.ordinal != SlayerTaskRegistry.tasks.indexOf(task) || player.slayerTask.remainingAmount <= 0)
        ) return "You need a slayer task to kill this monster."
        val required = LEVEL_REQUIREMENTS[npcId]
        if (required != null && player.skills.current(Skill.SLAYER) < required) {
            return "You need a slayer level of $required to harm this monster."
        }
        return null
    }

    /** 0 = none, 1 = melee mask bonus, 2 = imbued/ranged bonus. */
    fun damageBonus(player: SkillPlayer, npcId: Int, ranged: Boolean): Int {
        val task = SlayerTaskRegistry.forNpc(npcId) ?: return 0
        if (player.slayerTask.ordinal != SlayerTaskRegistry.tasks.indexOf(task) || player.slayerTask.remainingAmount <= 0) return 0
        return when (player.equipment.item(SkillEquipmentSlot.HEAD)) {
            BLACK_MASK, SLAYER_HELM -> if (!ranged) 1 else 0
            BLACK_MASK_IMBUED, SLAYER_HELM_IMBUED -> 2
            else -> 0
        }
    }

    fun hasSlayerHelmet(player: SkillPlayer): Boolean =
        player.equipment.item(SkillEquipmentSlot.HEAD) in intArrayOf(SLAYER_HELM, SLAYER_HELM_IMBUED)

    private val LEVEL_REQUIREMENTS = mapOf(2266 to 86, 6610 to 88, 4303 to 88, 4304 to 88, 3209 to 65, 3204 to 45, 3201 to 25)
    private const val BLACK_MASK = 8921
    private const val BLACK_MASK_IMBUED = 11784
    private const val SLAYER_HELM = 11864
    private const val SLAYER_HELM_IMBUED = 11865
}
