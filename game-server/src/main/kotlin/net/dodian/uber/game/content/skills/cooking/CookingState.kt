package net.dodian.uber.game.content.skills.cooking

import net.dodian.uber.game.content.skills.core.runtime.SkillActionState

data class CookingState(
    val itemId: Int,
    val cookIndex: Int,
    val remaining: Int,
) : SkillActionState
