package net.dodian.uber.game.systems.skills

import net.dodian.uber.game.content.skills.core.progression.SkillProgressionService
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.Skills

object ProgressionService {
    @JvmStatic
    fun addXp(player: Client, amount: Int, skill: Skill): Boolean = SkillProgressionService.gainXp(player, amount, skill)

    @JvmStatic
    fun gainXp(player: Client, amount: Int, skill: Skill): Boolean = addXp(player, amount, skill)

    @JvmStatic
    fun refresh(player: Client, skill: Skill) = SkillProgressionService.refresh(player, skill)

    @JvmStatic
    fun setSkillLevel(player: Client, skill: Skill, currentLevel: Int, experience: Int) =
        SkillProgressionService.setSkillLevel(player, skill, currentLevel, experience)

    @JvmStatic
    fun levelForXp(experience: Int): Int = Skills.getLevelForExperience(experience)

    @JvmStatic
    fun xpForLevel(level: Int): Int = Skills.getXPForLevel(level)
}
