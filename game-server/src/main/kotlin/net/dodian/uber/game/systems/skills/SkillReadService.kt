package net.dodian.uber.game.systems.skills

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.Skills

object SkillReadService {
    @JvmStatic
    fun experience(client: Client, skill: Skill): Int = client.getExperience(skill)

    @JvmStatic
    fun baseLevel(client: Client, skill: Skill): Int = Skills.getLevelForExperience(experience(client, skill))

    @JvmStatic
    fun effectiveLevel(client: Client, skill: Skill): Int {
        val base = baseLevel(client, skill)
        return when (skill) {
            Skill.HITPOINTS -> client.currentHealth
            Skill.PRAYER -> client.currentPrayer
            else -> base + client.boostedLevel[skill.id]
        }
    }

    @JvmStatic
    fun totalLevel(client: Client): Int =
        Skill.enabledSkills().mapToInt { skill -> baseLevel(client, skill) }.sum()
}
