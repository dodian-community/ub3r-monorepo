package net.dodian.uber.game.skills.cooking

import net.dodian.uber.game.skills.core.SkillActionState

data class CookingState(
    val itemId: Int,
    val cookIndex: Int,
    val remaining: Int,
) : SkillActionState
