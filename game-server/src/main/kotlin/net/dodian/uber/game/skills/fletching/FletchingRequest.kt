package net.dodian.uber.game.skills.fletching

import net.dodian.uber.game.skills.core.SkillActionRequest

data class FletchingRequest(
    val logIndex: Int,
    val productId: Int,
    val experience: Int,
    val amount: Int,
) : SkillActionRequest
