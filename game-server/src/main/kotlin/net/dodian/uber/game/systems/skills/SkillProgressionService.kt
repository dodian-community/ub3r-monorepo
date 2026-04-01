package net.dodian.uber.game.systems.skills

import net.dodian.uber.game.engine.config.gameMultiplierGlobalXp
import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.LevelUpEvent
import net.dodian.uber.game.events.skilling.SkillProgressAppliedEvent
import net.dodian.uber.game.model.UpdateFlag
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.Skills
import net.dodian.uber.game.netty.listener.out.RefreshSkill
import net.dodian.uber.game.persistence.player.PlayerSaveSegment

object SkillProgressionService {
    private const val MAX_XP = 200_000_000
    private val milestoneXp = intArrayOf(50_000_000, 75_000_000, 100_000_000, 125_000_000, 150_000_000, 175_000_000, 200_000_000)

    @JvmStatic
    fun apply(client: Client, request: SkillProgressionRequest): SkillProgressionResult {
        val result =
            when (request.mode) {
                SkillProgressionMode.GAIN_XP -> applyGainXp(client, request)
                SkillProgressionMode.SET_XP -> applySetXp(client, request)
                SkillProgressionMode.SET_LEVEL -> applySetLevel(client, request)
                SkillProgressionMode.REFRESH_ONLY -> {
                    refresh(client, request.skill)
                    val xp = client.getExperience(request.skill)
                    val lvl = Skills.getLevelForExperience(xp)
                    SkillProgressionResult(true, request.mode, request.skill, xp, xp, lvl, lvl, 0)
                }
            }
        if (result.success) {
            GameEventBus.post(
                SkillProgressAppliedEvent(
                    client = client,
                    skill = result.skill,
                    mode = result.mode,
                    oldExperience = result.oldExperience,
                    newExperience = result.newExperience,
                    oldLevel = result.oldLevel,
                    newLevel = result.newLevel,
                    appliedAmount = result.appliedAmount,
                ),
            )
        }
        return result
    }

    @JvmStatic
    fun gainXp(client: Client, amount: Int, skill: Skill): Boolean =
        apply(client, SkillProgressionRequest.gainXp(skill = skill, amount = amount)).success

    @JvmStatic
    fun refresh(client: Client, skill: Skill) {
        val baseLevel = Skills.getLevelForExperience(client.getExperience(skill))
        val level =
            when (skill) {
                Skill.HITPOINTS -> client.currentHealth
                Skill.PRAYER -> client.currentPrayer
                else -> baseLevel + client.boostedLevel[skill.id]
            }

        setSkillLevel(client, skill, level, client.getExperience(skill))
        client.setLevel(level, skill)
        client.send(RefreshSkill(skill, level, baseLevel, client.getExperience(skill)))
    }

    @JvmStatic
    fun setSkillLevel(client: Client, skill: Skill, currentLevel: Int, xp: Int) {
        client.sendString(maxOf(currentLevel, 0).toString(), skill.currentComponent)
        client.sendString(maxOf(Skills.getLevelForExperience(xp), 1).toString(), skill.levelComponent)
    }

    private fun applyGainXp(client: Client, request: SkillProgressionRequest): SkillProgressionResult {
        if (request.amount < 1) {
            val xp = client.getExperience(request.skill)
            val level = Skills.getLevelForExperience(xp)
            return SkillProgressionResult(false, request.mode, request.skill, xp, xp, level, level, 0)
        }

        val oldXp = client.getExperience(request.skill)
        val oldLevel = Skills.getLevelForExperience(oldXp)
        val requestedLong =
            request.amount.toLong() *
                (if (request.applyGlobalMultiplier) gameMultiplierGlobalXp.toLong() else 1L)
        if (requestedLong < 1L) {
            return SkillProgressionResult(false, request.mode, request.skill, oldXp, oldXp, oldLevel, oldLevel, 0)
        }
        val oldXpLong = oldXp.toLong()
        val targetLong =
            if (request.capExperience) {
                (oldXpLong + requestedLong).coerceAtMost(MAX_XP.toLong())
            } else {
                oldXpLong + requestedLong
            }
        val appliedLong = targetLong - oldXpLong
        val applied = appliedLong.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()

        client.addExperience(applied, request.skill)
        client.markSaveDirty(PlayerSaveSegment.STATS.mask)

        val newXp = client.getExperience(request.skill)
        val newLevel = Skills.getLevelForExperience(newXp)
        var animation = -1

        if (newLevel > oldLevel) {
            GameEventBus.post(
                LevelUpEvent(
                    client = client,
                    skill = request.skill,
                    oldLevel = oldLevel,
                    newLevel = newLevel,
                    oldExperience = oldXp,
                    newExperience = newXp,
                ),
            )
            animation = 199
            if (newLevel == 99) {
                animation = 623
                if (request.allowMilestoneBroadcasts) {
                    Client.publicyell("${client.playerName} has just reached the max level for ${request.skill.name}!")
                }
            } else if (newLevel > 90 && request.allowMilestoneBroadcasts) {
                Client.publicyell("${client.playerName}'s ${request.skill.name} level is now $newLevel!")
            }
            if (request.allowLevelUpMessage) {
                val article = if (request.skill == Skill.ATTACK || request.skill == Skill.AGILITY) "an" else "a"
                client.sendMessage("Congratulations, you just advanced $article ${request.skill.name} level.")
            }
        }

        if (request.allowMilestoneBroadcasts) {
            for (milestone in milestoneXp) {
                if (oldXp < milestone && newXp >= milestone) {
                    animation = 623
                    val milestoneText =
                        if (milestone == MAX_XP) "the maximum experience" else "${milestone / 1_000_000} million experience"
                    Client.publicyell("${client.playerName}'s ${request.skill.name} has just reached $milestoneText!")
                }
            }
        }

        client.setLevel(Skills.getLevelForExperience(client.getExperience(request.skill)), request.skill)
        refresh(client, request.skill)
        if (request.skill == Skill.FIREMAKING) {
            client.updateBonus(11)
        }
        if (request.skill == Skill.HITPOINTS && newLevel > client.maxHealth) {
            client.maxHealth = newLevel
        } else if (request.skill == Skill.PRAYER && newLevel > client.maxPrayer) {
            client.maxPrayer = newLevel
        }
        if (animation != -1 && request.playAnimation) {
            client.animation(animation, client.position)
        }
        if (request.updateAppearance) {
            client.updateFlags.setRequired(UpdateFlag.APPEARANCE, true)
        }

        return SkillProgressionResult(
            success = true,
            mode = request.mode,
            skill = request.skill,
            oldExperience = oldXp,
            newExperience = newXp,
            oldLevel = oldLevel,
            newLevel = newLevel,
            appliedAmount = applied,
        )
    }

    private fun applySetXp(client: Client, request: SkillProgressionRequest): SkillProgressionResult {
        val oldXp = client.getExperience(request.skill)
        val oldLevel = Skills.getLevelForExperience(oldXp)
        val target = if (request.capExperience) request.amount.coerceIn(0, MAX_XP) else request.amount.coerceAtLeast(0)
        client.setExperience(target, request.skill)
        client.setLevel(Skills.getLevelForExperience(target), request.skill)
        client.markSaveDirty(PlayerSaveSegment.STATS.mask)
        refresh(client, request.skill)
        val newLevel = Skills.getLevelForExperience(target)
        return SkillProgressionResult(true, request.mode, request.skill, oldXp, target, oldLevel, newLevel, target - oldXp)
    }

    private fun applySetLevel(client: Client, request: SkillProgressionRequest): SkillProgressionResult {
        val oldXp = client.getExperience(request.skill)
        val oldLevel = Skills.getLevelForExperience(oldXp)
        val level = request.amount.coerceAtLeast(1)
        client.setLevel(level, request.skill)
        client.markSaveDirty(PlayerSaveSegment.STATS.mask)
        refresh(client, request.skill)
        return SkillProgressionResult(true, request.mode, request.skill, oldXp, oldXp, oldLevel, level, level - oldLevel)
    }
}
