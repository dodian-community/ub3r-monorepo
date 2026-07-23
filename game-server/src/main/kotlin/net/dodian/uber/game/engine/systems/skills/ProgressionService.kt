package net.dodian.uber.game.engine.systems.skills

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.Skills
import net.dodian.uber.game.api.plugin.skills.SkillPlayer

object ProgressionService {
    /** Public content API.  Client overloads are retained for engine/Java call sites only. */
    @JvmStatic
    fun addXp(player: SkillPlayer, amount: Int, skill: Skill): Boolean =
        player.skills.gainXp(amount, skill)

    @JvmStatic
    fun addXp(player: Client, amount: Int, skill: Skill): Boolean = SkillProgressionService.gainXp(player, amount, skill)

    @JvmStatic
    fun gainXp(player: Client, amount: Int, skill: Skill): Boolean = addXp(player, amount, skill)

    @JvmStatic
    fun refresh(player: Client, skill: Skill) = SkillProgressionService.refresh(player, skill)

    @JvmStatic
    fun levelForXp(experience: Int): Int = Skills.getLevelForExperience(experience)

    @JvmStatic
    fun xpForLevel(level: Int): Int = Skills.getXPForLevel(level)
}
