package net.dodian.uber.game.content.skills.fletching

import net.dodian.uber.game.systems.skills.SkillActionState

data class FletchingState(
    val logIndex: Int,
    val productId: Int = -1,
    val experience: Int = 0,
    val remaining: Int = 0,
) : SkillActionState {
    val isActive: Boolean
        get() = productId > 0 && remaining > 0
}
