package net.dodian.uber.game.content.skills.herblore

data class HerbloreBatchRequest(
    val slot: Int,
    val amount: Int,
    val npcId: Int,
)

data class HerbDefinition(
    val grimyId: Int,
    val cleanId: Int,
    val unfinishedPotionId: Int,
    val requiredLevel: Int,
    val cleaningExperience: Int,
    val premiumOnly: Boolean,
)

data class PotionRecipeDefinition(
    val unfinishedPotionId: Int,
    val secondaryId: Int,
    val finishedPotionId: Int,
    val requiredLevel: Int,
    val experience: Int,
    val premiumOnly: Boolean,
)

data class PotionDoseDefinition(
    val oneDoseId: Int,
    val twoDoseId: Int,
    val threeDoseId: Int,
    val fourDoseId: Int,
)

object HerbloreData {
    const val BATCH_INTERFACE_ID: Int = 4753
    const val VIAL_OF_WATER_ID: Int = 228
    const val EMPTY_VIAL_ID: Int = 229
    const val UNFINISHED_POTION_VIAL_ID: Int = 227
    const val GRIND_COST_PER_HERB: Int = 200
    const val UNFINISHED_POTION_COST: Int = 1_000

    @JvmField
    val herbDefinitions: List<HerbDefinition> = listOf(
        HerbDefinition(199, 249, 91, 1, 24, false),
        HerbDefinition(203, 253, 95, 10, 40, false),
        HerbDefinition(207, 257, 99, 25, 56, false),
        HerbDefinition(209, 259, 101, 40, 72, true),
        HerbDefinition(213, 263, 105, 54, 88, true),
        HerbDefinition(3051, 3000, 3004, 59, 96, true),
        HerbDefinition(215, 265, 107, 65, 104, true),
        HerbDefinition(217, 267, 109, 70, 120, true),
        HerbDefinition(219, 269, 111, 75, 136, true),
    )

    @JvmField
    val potionRecipes: List<PotionRecipeDefinition> = listOf(
        PotionRecipeDefinition(91, 221, 121, 3, 200, false),
        PotionRecipeDefinition(95, 225, 115, 14, 480, false),
        PotionRecipeDefinition(99, 239, 133, 25, 560, false),
        PotionRecipeDefinition(99, 231, 139, 38, 700, false),
        PotionRecipeDefinition(101, 221, 145, 46, 840, true),
        PotionRecipeDefinition(105, 225, 157, 55, 1000, true),
        PotionRecipeDefinition(3004, 223, 3026, 60, 1120, true),
        PotionRecipeDefinition(107, 239, 163, 65, 1200, true),
        PotionRecipeDefinition(109, 245, 169, 75, 1350, true),
        PotionRecipeDefinition(111, 6045, 2454, 79, 1425, true),
    )

    @JvmField
    val potionDoseDefinitions: List<PotionDoseDefinition> = listOf(
        PotionDoseDefinition(119, 117, 115, 113),
        PotionDoseDefinition(125, 123, 121, 2428),
        PotionDoseDefinition(137, 135, 133, 2432),
        PotionDoseDefinition(143, 141, 139, 2434),
        PotionDoseDefinition(149, 147, 145, 2436),
        PotionDoseDefinition(161, 159, 157, 2440),
        PotionDoseDefinition(167, 165, 163, 2442),
        PotionDoseDefinition(173, 171, 169, 2444),
        PotionDoseDefinition(3030, 3028, 3026, 3024),
        PotionDoseDefinition(2458, 2456, 2454, 2452),
        PotionDoseDefinition(12701, 12699, 12697, 12695),
        PotionDoseDefinition(11733, 11732, 11731, 11730),
    )

    @JvmStatic
    fun findHerbDefinitionByGrimy(grimyId: Int): HerbDefinition? = herbDefinitions.firstOrNull { it.grimyId == grimyId }

    @JvmStatic
    fun findHerbDefinitionByClean(cleanId: Int): HerbDefinition? = herbDefinitions.firstOrNull { it.cleanId == cleanId }

    @JvmStatic
    fun findPotionRecipe(unfinishedPotionId: Int, secondaryId: Int): PotionRecipeDefinition? =
        potionRecipes.firstOrNull { it.unfinishedPotionId == unfinishedPotionId && it.secondaryId == secondaryId }

    @JvmStatic
    fun findPotionDoseByAny(itemId: Int): PotionDoseDefinition? =
        potionDoseDefinitions.firstOrNull {
            it.oneDoseId == itemId || it.twoDoseId == itemId || it.threeDoseId == itemId || it.fourDoseId == itemId
        }
}
