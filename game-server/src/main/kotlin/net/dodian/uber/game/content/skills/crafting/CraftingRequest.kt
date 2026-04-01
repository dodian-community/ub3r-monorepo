package net.dodian.uber.game.content.skills.crafting

import net.dodian.uber.game.systems.skills.SkillActionRequest

data class CraftingRequest(
    val mode: CraftingMode,
    val selectedItemId: Int = -1,
    val productId: Int = -1,
    val amount: Int = 0,
    val requiredLevel: Int = 0,
    val experience: Int = 0,
) : SkillActionRequest
