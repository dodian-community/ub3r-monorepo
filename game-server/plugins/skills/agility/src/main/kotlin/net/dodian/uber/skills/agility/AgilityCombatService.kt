package net.dodian.uber.skills.agility

import net.dodian.uber.game.api.plugin.skills.SkillPlayer
import net.dodian.uber.game.model.player.skills.Skill

/** Agility's combat critical-chance contribution. */
object AgilityCombatService {
    fun criticalChance(player: SkillPlayer): Int = player.skills.current(Skill.AGILITY) / 9

    /** Required Agility level for agility-gated equipment, or null when unrestricted. */
    @JvmStatic fun equipmentLevelRequirement(itemId: Int): Int? = if (itemId == 4224) 60 else null
}
