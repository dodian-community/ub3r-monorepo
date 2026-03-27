package net.dodian.uber.game.content.skills.core.progression

import net.dodian.uber.game.model.player.skills.Skill

enum class SkillProgressionMode {
    GAIN_XP,
    SET_XP,
    SET_LEVEL,
    REFRESH_ONLY,
}

data class SkillProgressionRequest(
    val skill: Skill,
    val mode: SkillProgressionMode,
    val amount: Int = 0,
    val applyGlobalMultiplier: Boolean = true,
    val capExperience: Boolean = true,
    val allowMilestoneBroadcasts: Boolean = true,
    val allowLevelUpMessage: Boolean = true,
    val playAnimation: Boolean = true,
    val updateAppearance: Boolean = true,
) {
    companion object {
        @JvmStatic
        fun gainXp(
            skill: Skill,
            amount: Int,
            applyGlobalMultiplier: Boolean = true,
            allowMilestoneBroadcasts: Boolean = true,
            allowLevelUpMessage: Boolean = true,
            playAnimation: Boolean = true,
            updateAppearance: Boolean = true,
        ): SkillProgressionRequest =
            SkillProgressionRequest(
                skill = skill,
                mode = SkillProgressionMode.GAIN_XP,
                amount = amount,
                applyGlobalMultiplier = applyGlobalMultiplier,
                allowMilestoneBroadcasts = allowMilestoneBroadcasts,
                allowLevelUpMessage = allowLevelUpMessage,
                playAnimation = playAnimation,
                updateAppearance = updateAppearance,
            )

        @JvmStatic
        fun setXp(
            skill: Skill,
            amount: Int,
            capExperience: Boolean = true,
        ): SkillProgressionRequest =
            SkillProgressionRequest(
                skill = skill,
                mode = SkillProgressionMode.SET_XP,
                amount = amount,
                capExperience = capExperience,
                applyGlobalMultiplier = false,
                allowMilestoneBroadcasts = false,
                allowLevelUpMessage = false,
                playAnimation = false,
            )

        @JvmStatic
        fun setLevel(
            skill: Skill,
            amount: Int,
        ): SkillProgressionRequest =
            SkillProgressionRequest(
                skill = skill,
                mode = SkillProgressionMode.SET_LEVEL,
                amount = amount,
                applyGlobalMultiplier = false,
                capExperience = false,
                allowMilestoneBroadcasts = false,
                allowLevelUpMessage = false,
                playAnimation = false,
            )

        @JvmStatic
        fun refreshOnly(skill: Skill): SkillProgressionRequest =
            SkillProgressionRequest(
                skill = skill,
                mode = SkillProgressionMode.REFRESH_ONLY,
                applyGlobalMultiplier = false,
                capExperience = false,
                allowMilestoneBroadcasts = false,
                allowLevelUpMessage = false,
                playAnimation = false,
                updateAppearance = false,
            )
    }
}
