package net.dodian.uber.game.content.skills.cooking

import net.dodian.uber.game.systems.skills.SkillActionRequest

data class CookingRequest(
    val itemId: Int,
    val cookIndex: Int,
    val amount: Int,
) : SkillActionRequest
