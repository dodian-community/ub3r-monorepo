package net.dodian.uber.game.content.skills.fletching

import net.dodian.uber.game.systems.skills.SkillActionRequest

data class FletchingRequest(
    val logIndex: Int,
    val productId: Int,
    val experience: Int,
    val amount: Int,
) : SkillActionRequest
