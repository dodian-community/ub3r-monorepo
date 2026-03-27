package net.dodian.uber.game.content.skills.core.progression

import net.dodian.uber.game.systems.world.player.PlayerRegistry
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.Skills
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.persistence.command.CommandDbService

object SkillAdminService {
    @JvmStatic
    fun set(client: Client, skill: Skill, level: Int? = null, experience: Int? = null) {
        if (experience != null) {
            SkillProgressionService.apply(client, SkillProgressionRequest.setXp(skill = skill, amount = experience))
        }
        if (level != null) {
            SkillProgressionService.apply(client, SkillProgressionRequest.setLevel(skill = skill, amount = level))
        } else if (experience != null) {
            SkillProgressionService.refresh(client, skill)
        }
    }

    @JvmStatic
    fun addXp(client: Client, skill: Skill, amount: Int, applyGlobalMultiplier: Boolean = false): Boolean =
        SkillProgressionService.apply(
            client,
            SkillProgressionRequest.gainXp(
                skill = skill,
                amount = amount,
                applyGlobalMultiplier = applyGlobalMultiplier,
                allowMilestoneBroadcasts = true,
                allowLevelUpMessage = true,
            ),
        ).success

    @JvmStatic
    fun reset(client: Client) {
        Skill.values()
            .filterNotNull()
            .forEach { skill ->
                val level = if (skill == Skill.HITPOINTS) 10 else 1
                val xp = if (skill == Skill.HITPOINTS) 1155 else Skills.getXPForLevel(level)
                set(client, skill, level = level, experience = xp)
            }
    }

    @JvmStatic
    fun removeExperienceFromPlayer(
        moderator: Client,
        user: String,
        skillId: Int,
        experienceToRemove: Int,
    ) {
        val skill = Skill.getSkill(skillId) ?: return
        val skillName = skill.getName()
        val safeAmount = experienceToRemove.coerceAtLeast(0)
        val online = PlayerRegistry.getPlayer(user) as? Client
        if (online != null) {
            val currentXp = online.getExperience(skill)
            val removed = minOf(currentXp, safeAmount)
            SkillProgressionService.apply(
                online,
                SkillProgressionRequest.setXp(skill = skill, amount = currentXp - removed),
            )
            moderator.send(
                SendMessage(
                    "Removed $removed/$currentXp xp from $user's $skillName(id:$skillId)!",
                ),
            )
            return
        }

        CommandDbService.submit(
            "remove-skill",
            { CommandDbService.removeOfflineExperience(user, skillName, safeAmount) },
            { result ->
                if (moderator.disconnected) {
                    return@submit
                }
                if (result.status == CommandDbService.OfflineSkillMutationResult.Status.NOT_FOUND) {
                    moderator.sendMessage("username '$user' have yet to login!")
                    return@submit
                }
                moderator.send(
                    SendMessage(
                        "Removed ${result.removedXp}/${result.currentXp} xp from $user's $skillName(id:$skillId)!",
                    ),
                )
            },
            { exception ->
                if (!moderator.disconnected) {
                    moderator.sendMessage("Could not update that player's skill right now.")
                }
            },
        )
    }
}
