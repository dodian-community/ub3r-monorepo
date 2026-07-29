package net.dodian.uber.skills.agility

import net.dodian.uber.game.api.plugin.skills.SkillPlayer
import net.dodian.uber.game.model.player.skills.Skill

/** Agility's combat critical-chance contribution. */
object AgilityCombatService {
    fun criticalChance(player: SkillPlayer): Int = player.skills.current(Skill.AGILITY) / 9
}
