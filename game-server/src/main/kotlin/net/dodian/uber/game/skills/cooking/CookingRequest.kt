package net.dodian.uber.game.skills.cooking

import net.dodian.uber.game.skills.core.SkillActionRequest

data class CookingRequest(
    val itemId: Int,
    val cookIndex: Int,
    val amount: Int,
) : SkillActionRequest
