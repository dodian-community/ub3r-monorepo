package net.dodian.uber.game.content.objects.impl.mining

enum class RockCategory {
    STANDARD,
    GEM,
    SPECIAL,
}

data class MiningRockDef(
    val objectId: Int,
    val requiredLevel: Int,
    val baseDelayMs: Long,
    val oreItemId: Int,
    val experience: Int,
    val displayNameOverride: String? = null,
    val randomGemEligible: Boolean = true,
    val restThreshold: Int,
    val category: RockCategory = RockCategory.STANDARD,
)

data class PickaxeDef(
    val itemId: Int,
    val requiredLevel: Int,
    val speedBonus: Double,
    val animationId: Int,
    val dragonTierBoostEligible: Boolean,
)

object MiningDefinitions {
    val standardRocks: List<MiningRockDef> =
        listOf(
            MiningRockDef(7471, 1, 1000L, 1436, 50, randomGemEligible = false, restThreshold = 14),
            MiningRockDef(7451, 1, 2000L, 436, 110, restThreshold = 4),
            MiningRockDef(7484, 1, 2000L, 436, 110, restThreshold = 4),
            MiningRockDef(7452, 1, 2000L, 438, 110, restThreshold = 4),
            MiningRockDef(7485, 1, 2000L, 438, 110, restThreshold = 4),
            MiningRockDef(7455, 15, 3000L, 440, 280, restThreshold = 4),
            MiningRockDef(7488, 15, 3000L, 440, 280, restThreshold = 4),
            MiningRockDef(7456, 30, 5000L, 453, 420, restThreshold = 4),
            MiningRockDef(7489, 30, 5000L, 453, 420, restThreshold = 4),
            MiningRockDef(7458, 40, 6000L, 444, 510, restThreshold = 4),
            MiningRockDef(7491, 40, 6000L, 444, 510, restThreshold = 4),
            MiningRockDef(7459, 55, 7000L, 447, 620, restThreshold = 4),
            MiningRockDef(7492, 55, 7000L, 447, 620, restThreshold = 4),
            MiningRockDef(7460, 70, 9000L, 449, 780, restThreshold = 4),
            MiningRockDef(7493, 70, 9000L, 449, 780, restThreshold = 4),
            MiningRockDef(7461, 85, 35000L, 451, 3100, restThreshold = 4),
            MiningRockDef(7494, 85, 35000L, 451, 3100, restThreshold = 4),
        )

    val rocksByObjectId: Map<Int, MiningRockDef> = standardRocks.associateBy { it.objectId }

    // Preserve old findPick ordering by storing the strongest entries first.
    val pickaxesDescending: List<PickaxeDef> =
        listOf(
            PickaxeDef(20014, 61, 0.8, 7139, dragonTierBoostEligible = true),
            PickaxeDef(11920, 61, 0.8, 7139, dragonTierBoostEligible = true),
            PickaxeDef(1275, 41, 0.42, 624, dragonTierBoostEligible = false),
            PickaxeDef(1271, 31, 0.33, 628, dragonTierBoostEligible = false),
            PickaxeDef(1273, 21, 0.24, 629, dragonTierBoostEligible = false),
            PickaxeDef(12297, 11, 0.15, 629, dragonTierBoostEligible = false),
            PickaxeDef(1269, 6, 0.1, 627, dragonTierBoostEligible = false),
            PickaxeDef(1267, 1, 0.065, 626, dragonTierBoostEligible = false),
            PickaxeDef(1265, 1, 0.04, 625, dragonTierBoostEligible = false),
        )

    val pickaxeByItemId: Map<Int, PickaxeDef> = pickaxesDescending.associateBy { it.itemId }

    val randomGemDropTable: IntArray = intArrayOf(1623, 1623, 1623, 1621, 1621, 1619, 1617)
}
