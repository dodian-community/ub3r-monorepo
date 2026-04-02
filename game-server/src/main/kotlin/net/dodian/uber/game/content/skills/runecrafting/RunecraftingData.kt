package net.dodian.uber.game.content.skills.runecrafting

data class RunecraftingRequest(
    val runeId: Int,
    val requiredLevel: Int,
    val experiencePerEssence: Int,
)

data class RunecraftingState(
    val lastAltarCraftAtMillis: Long = 0L,
)

data class RunecraftingAltarDefinition(
    val objectId: Int,
    val request: RunecraftingRequest,
)

object RunecraftingData {
    const val RUNE_ESSENCE_ID = 1436

    private val altarDefinitions: List<RunecraftingAltarDefinition> =
        listOf(
            RunecraftingAltarDefinition(
                objectId = 14905,
                request = RunecraftingRequest(runeId = 561, requiredLevel = 1, experiencePerEssence = 60),
            ),
            RunecraftingAltarDefinition(
                objectId = 27978,
                request = RunecraftingRequest(runeId = 565, requiredLevel = 50, experiencePerEssence = 85),
            ),
            RunecraftingAltarDefinition(
                objectId = 14903,
                request = RunecraftingRequest(runeId = 564, requiredLevel = 75, experiencePerEssence = 120),
            ),
        )

    @JvmField
    val altarObjectIds: IntArray = altarDefinitions.map { it.objectId }.toIntArray()

    @JvmStatic
    fun byObjectId(objectId: Int): RunecraftingAltarDefinition? = altarDefinitions.firstOrNull { it.objectId == objectId }
}
