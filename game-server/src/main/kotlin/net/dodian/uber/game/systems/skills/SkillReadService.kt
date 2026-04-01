package net.dodian.uber.game.systems.skills

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill

object SkillReadService {
    @JvmStatic
    fun experience(client: Client, skill: Skill): Int =
        net.dodian.uber.game.content.skills.core.progression.SkillReadService.experience(client, skill)

    @JvmStatic
    fun baseLevel(client: Client, skill: Skill): Int =
        net.dodian.uber.game.content.skills.core.progression.SkillReadService.baseLevel(client, skill)

    @JvmStatic
    fun effectiveLevel(client: Client, skill: Skill): Int =
        net.dodian.uber.game.content.skills.core.progression.SkillReadService.effectiveLevel(client, skill)

    @JvmStatic
    fun totalLevel(client: Client): Int =
        net.dodian.uber.game.content.skills.core.progression.SkillReadService.totalLevel(client)
}
