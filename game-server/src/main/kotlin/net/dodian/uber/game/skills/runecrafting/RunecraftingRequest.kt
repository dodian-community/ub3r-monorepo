package net.dodian.uber.game.skills.runecrafting

data class RunecraftingRequest(
    val runeId: Int,
    val requiredLevel: Int,
    val experiencePerEssence: Int,
)
