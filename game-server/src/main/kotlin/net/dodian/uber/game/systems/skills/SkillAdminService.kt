package net.dodian.uber.game.systems.skills

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill

object SkillAdminService {
    @JvmStatic
    fun set(client: Client, skill: Skill, level: Int? = null, experience: Int? = null) =
        net.dodian.uber.game.content.skills.core.progression.SkillAdminService.set(client, skill, level, experience)

    @JvmStatic
    fun addXp(client: Client, skill: Skill, amount: Int, applyGlobalMultiplier: Boolean = false): Boolean =
        net.dodian.uber.game.content.skills.core.progression.SkillAdminService.addXp(client, skill, amount, applyGlobalMultiplier)

    @JvmStatic
    fun reset(client: Client) =
        net.dodian.uber.game.content.skills.core.progression.SkillAdminService.reset(client)

    @JvmStatic
    fun removeExperienceFromPlayer(moderator: Client, user: String, skillId: Int, experienceToRemove: Int) =
        net.dodian.uber.game.content.skills.core.progression.SkillAdminService.removeExperienceFromPlayer(
            moderator,
            user,
            skillId,
            experienceToRemove,
        )
}
