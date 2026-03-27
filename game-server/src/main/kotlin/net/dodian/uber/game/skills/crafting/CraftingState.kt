package net.dodian.uber.game.skills.crafting

import net.dodian.uber.game.skills.core.runtime.SkillActionState

enum class CraftingMode {
    LEATHER,
    SPINNING,
    SHAFTING,
}

data class CraftingState(
    val mode: CraftingMode,
    val selectedItemId: Int = -1,
    val productId: Int = -1,
    val remaining: Int = 0,
    val requiredLevel: Int = 0,
    val experience: Int = 0,
) : SkillActionState
