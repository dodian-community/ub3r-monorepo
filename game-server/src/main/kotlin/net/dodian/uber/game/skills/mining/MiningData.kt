package net.dodian.uber.game.skills.mining

enum class RockCategory {
    STANDARD,
    GEM,
    SPECIAL,
}

data class MiningRockDef(
    val name: String,
    val objectIds: IntArray,
    val requiredLevel: Int,
    val baseDelayMs: Long,
    val oreItemId: Int,
    val experience: Int,
    val randomGemEligible: Boolean = true,
    val restThreshold: Int,
    val category: RockCategory = RockCategory.STANDARD,
)

data class PickaxeDef(
    val name: String,
    val itemId: Int,
    val requiredLevel: Int,
    val speedBonus: Double,
    val animationId: Int,
    val dragonTierBoostEligible: Boolean,
)

object MiningData {
    // This mirrors the original active Dodian mining content from the old Utils arrays.
    // The commented-out gem rock ids (7464/7463) were never active in the original mining loop,
    // so they remain out of this parity data set.
    val rocks: List<MiningRockDef> =
        listOf(
            essence("Rune essence", 7471),
            ore("Copper", 436, level = 1, delayMs = 2000L, xp = 110, 7451, 7484),
            ore("Tin", 438, level = 1, delayMs = 2000L, xp = 110, 7452, 7485),
            ore("Iron", 440, level = 15, delayMs = 3000L, xp = 280, 7455, 7488),
            ore("Coal", 453, level = 30, delayMs = 5000L, xp = 420, 7456, 7489),
            ore("Gold", 444, level = 40, delayMs = 6000L, xp = 510, 7458, 7491),
            ore("Mithril", 447, level = 55, delayMs = 7000L, xp = 620, 7459, 7492),
            ore("Adamantite", 449, level = 70, delayMs = 9000L, xp = 780, 7460, 7493),
            ore("Runite", 451, level = 85, delayMs = 35000L, xp = 3100, 7461, 7494),
        )

    val rockByObjectId: Map<Int, MiningRockDef> =
        buildMap {
            rocks.forEach { rock ->
                rock.objectIds.forEach { objectId ->
                    put(objectId, rock)
                }
            }
        }

    val allRockObjectIds: IntArray = rockByObjectId.keys.sorted().toIntArray()

    // Preserve the old findPick ordering by listing strongest pickaxes first.
    val pickaxesDescending: List<PickaxeDef> =
        listOf(
            pickaxe("3rd age", 20014, level = 61, bonus = 0.8, animation = 7139, dragonBoost = true),
            pickaxe("Dragon", 11920, level = 61, bonus = 0.8, animation = 7139, dragonBoost = true),
            pickaxe("Rune", 1275, level = 41, bonus = 0.42, animation = 624),
            pickaxe("Iron", 1271, level = 31, bonus = 0.33, animation = 628),
            pickaxe("Steel", 1273, level = 21, bonus = 0.24, animation = 629),
            pickaxe("Black", 12297, level = 11, bonus = 0.15, animation = 629),
            pickaxe("Mithril", 1269, level = 6, bonus = 0.1, animation = 627),
            pickaxe("Adamant", 1267, level = 1, bonus = 0.065, animation = 626),
            pickaxe("Bronze", 1265, level = 1, bonus = 0.04, animation = 625),
        )

    val pickaxeByItemId: Map<Int, PickaxeDef> = pickaxesDescending.associateBy { it.itemId }

    val randomGemDropTable: IntArray = intArrayOf(1623, 1623, 1623, 1621, 1621, 1619, 1617)

    private fun essence(name: String, vararg objectIds: Int): MiningRockDef =
        MiningRockDef(
            name = name,
            objectIds = objectIds,
            requiredLevel = 1,
            baseDelayMs = 1000L,
            oreItemId = 1436,
            experience = 50,
            randomGemEligible = false,
            restThreshold = 14,
        )

    private fun ore(
        name: String,
        oreItemId: Int,
        level: Int,
        delayMs: Long,
        xp: Int,
        vararg objectIds: Int,
    ): MiningRockDef =
        MiningRockDef(
            name = name,
            objectIds = objectIds,
            requiredLevel = level,
            baseDelayMs = delayMs,
            oreItemId = oreItemId,
            experience = xp,
            restThreshold = 4,
        )

    private fun pickaxe(
        name: String,
        itemId: Int,
        level: Int,
        bonus: Double,
        animation: Int,
        dragonBoost: Boolean = false,
    ): PickaxeDef =
        PickaxeDef(
            name = name,
            itemId = itemId,
            requiredLevel = level,
            speedBonus = bonus,
            animationId = animation,
            dragonTierBoostEligible = dragonBoost,
        )
}
