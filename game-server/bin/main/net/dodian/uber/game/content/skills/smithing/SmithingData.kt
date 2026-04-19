package net.dodian.uber.game.content.skills.smithing

object SmithingData {
    private val smithingPageSlots = mapOf(
        1119 to intArrayOf(0, 4, 8, 9, 18),
        1120 to intArrayOf(1, 2, 13, 14, 17),
        1121 to intArrayOf(15, 20, 19, 21, 22),
        1122 to intArrayOf(3, 10, 12, 16, 6),
        1123 to intArrayOf(5, 7, 11, -1, -1),
    )

    private val smeltFrameIds = intArrayOf(2405, 2406, 2407, 2409, 2410, 2411, 2412, 2413)

    val smeltingRecipes: List<SmeltingRecipe> = smeltingRecipes {
        bar(2349) {
            level(1)
            xp(120)
            ore(436)
            ore(438)
        }
        bar(2351) {
            level(15)
            xp(260)
            ore(440)
            successChance(54)
            failureMessage("You fail to refine the iron")
        }
        bar(2355) {
            level(20)
            xp(280)
            ore(442)
        }
        bar(2353) {
            level(30)
            xp(360)
            ore(440)
            ore(453, 2)
        }
        bar(2357) {
            level(40)
            xp(460)
            ore(444)
        }
        bar(2359) {
            level(55)
            xp(600)
            ore(447)
            ore(453, 3)
        }
        bar(2361) {
            level(70)
            xp(760)
            ore(449)
            ore(453, 4)
        }
        bar(2363) {
            level(85)
            xp(1000)
            ore(451)
            ore(453, 6)
        }
    }

    private val classicSmeltingButtonMappings: List<FurnaceButtonMapping> = listOf(
        15147, 15146, 10247, 9110,
        15151, 15150, 15149, 15148,
        15155, 15154, 15153, 15152,
        15159, 15158, 15157, 15156,
        15163, 15162, 15161, 15160,
        29017, 29016, 24253, 16062,
        29022, 29020, 29019, 29018,
        29026, 29025, 29024, 29023,
    ).mapIndexed { index, buttonId ->
        val amount = when (index % 4) {
            0 -> 1
            1 -> 5
            2 -> 10
            else -> 28
        }
        val recipeIndex = index / 4
        FurnaceButtonMapping(buttonId, smeltingRecipes[recipeIndex].barId, amount)
    }

    private val mysticSmeltingButtonMappings: List<FurnaceButtonMapping> =
        SmeltingButtons.all.all.flatMap { it.toMappings() }

    val smeltingButtonMappings: List<FurnaceButtonMapping> = classicSmeltingButtonMappings + mysticSmeltingButtonMappings

    val smithingTiers: List<SmithingTier> = listOf(
        buildTier(1, "Bronze", 2349, SmithingFrameDefinitions.smithingFrame[0]),
        buildTier(2, "Iron", 2351, SmithingFrameDefinitions.smithingFrame[1]),
        buildTier(3, "Steel", 2353, SmithingFrameDefinitions.smithingFrame[2]),
        buildTier(4, "Mithril", 2359, SmithingFrameDefinitions.smithingFrame[3]),
        buildTier(5, "Adamant", 2361, SmithingFrameDefinitions.smithingFrame[4]),
        buildTier(6, "Rune", 2363, SmithingFrameDefinitions.smithingFrame[5]),
    )

    @JvmStatic
    fun findSmeltingRecipe(barId: Int): SmeltingRecipe? = smeltingRecipes.firstOrNull { it.barId == barId }

    @JvmStatic
    fun findSmeltingRecipeByOre(itemId: Int): SmeltingRecipe? =
        smeltingRecipes.firstOrNull { recipe -> recipe.oreRequirements.any { it.itemId == itemId } }

    @JvmStatic
    fun findSmithingTierByBar(barId: Int): SmithingTier? = smithingTiers.firstOrNull { it.barId == barId }

    @JvmStatic
    fun findSmithingTierByTypeId(typeId: Int): SmithingTier? = smithingTiers.firstOrNull { it.typeId == typeId }

    @JvmStatic
    fun findTierForProduct(itemId: Int): SmithingTier? = smithingTiers.firstOrNull { tier -> tier.products.any { it.itemId == itemId } }

    @JvmStatic
    fun displayItemsForFrame(tier: SmithingTier, frameId: Int): List<SmithingDisplayItem> {
        val indices = smithingPageSlots[frameId] ?: return emptyList()
        return indices.map { index ->
            val product = tier.products.getOrNull(index)
            if (product == null) {
                SmithingDisplayItem(-1, 0)
            } else {
                SmithingDisplayItem(product.itemId, product.outputAmount)
            }
        }
    }

    @JvmStatic
    fun frameIds(): IntArray = smeltFrameIds.copyOf()

    @JvmStatic
    fun isSmeltingInterfaceButton(buttonId: Int): Boolean =
        smeltingButtonMappings.any { it.buttonId == buttonId } || smeltFrameIds.contains(buttonId)

    private fun buildTier(typeId: Int, displayName: String, barId: Int, frame: Array<SmithingFrameEntry>): SmithingTier {
        val products = frame.map { entry ->
            SmithingProduct(
                itemId = entry.itemId,
                outputAmount = entry.outputAmount,
                levelRequired = entry.levelRequired,
                barsRequired = entry.barsRequired,
                barCountLineId = entry.barCountLineId,
                itemNameLineId = entry.itemNameLineId,
            )
        }
        return SmithingTier(typeId, displayName, barId, products)
    }
}

data class FurnaceButtonMapping(
    val buttonId: Int,
    val barId: Int,
    val amount: Int,
)

data class OreRequirement(
    val itemId: Int,
    val amount: Int,
)

data class SmeltingButtonSet(
    val displayName: String,
    val barId: Int,
    val oneButtonId: Int,
    val fiveButtonId: Int,
    val tenButtonId: Int,
    val xButtonId: Int,
) {
    fun toMappings(): List<FurnaceButtonMapping> = listOf(
        FurnaceButtonMapping(oneButtonId, barId, 1),
        FurnaceButtonMapping(fiveButtonId, barId, 5),
        FurnaceButtonMapping(tenButtonId, barId, 10),
        FurnaceButtonMapping(xButtonId, barId, 0),
    ).filter { it.buttonId > 0 }
}

data class NamedSmeltingButtonSet(
    val bronze: SmeltingButtonSet,
    val iron: SmeltingButtonSet,
    val silver: SmeltingButtonSet,
    val steel: SmeltingButtonSet,
    val gold: SmeltingButtonSet,
    val mithril: SmeltingButtonSet,
    val adamant: SmeltingButtonSet,
    val rune: SmeltingButtonSet,
) {
    val all: List<SmeltingButtonSet> = listOf(
        bronze,
        iron,
        silver,
        steel,
        gold,
        mithril,
        adamant,
        rune,
    )
}

object SmeltingButtons {
    val all = NamedSmeltingButtonSet(
        bronze = SmeltingButtonSet(
            displayName = "Bronze",
            barId = 2349,
            oneButtonId = 3987,
            fiveButtonId = 3986,
            tenButtonId = 2807,
            xButtonId = 2414,
        ),
        iron = SmeltingButtonSet(
            displayName = "Iron",
            barId = 2351,
            oneButtonId = 3991,
            fiveButtonId = 3990,
            tenButtonId = 3989,
            xButtonId = 3988,
        ),
        silver = SmeltingButtonSet(
            displayName = "Silver",
            barId = 2355,
            oneButtonId = 3995,
            fiveButtonId = 3994,
            tenButtonId = 3993,
            xButtonId = 3992,
        ),
        steel = SmeltingButtonSet(
            displayName = "Steel",
            barId = 2353,
            oneButtonId = 3999,
            fiveButtonId = 3998,
            tenButtonId = 3997,
            xButtonId = 3996,
        ),
        gold = SmeltingButtonSet(
            displayName = "Gold",
            barId = 2357,
            oneButtonId = 4003,
            fiveButtonId = 4002,
            tenButtonId = 4001,
            xButtonId = 4000,
        ),
        mithril = SmeltingButtonSet(
            displayName = "Mithril",
            barId = 2359,
            oneButtonId = 7441,
            fiveButtonId = 7440,
            tenButtonId = 6397,
            xButtonId = 4158,
        ),
        adamant = SmeltingButtonSet(
            displayName = "Adamant",
            barId = 2361,
            oneButtonId = 7446,
            fiveButtonId = 7444,
            tenButtonId = 7443,
            xButtonId = 7442,
        ),
        rune = SmeltingButtonSet(
            displayName = "Rune",
            barId = 2363,
            oneButtonId = 7450,
            fiveButtonId = 7449,
            tenButtonId = 7448,
            xButtonId = 7447,
        ),
    )

    val bronze: SmeltingButtonSet get() = all.bronze
    val iron: SmeltingButtonSet get() = all.iron
    val silver: SmeltingButtonSet get() = all.silver
    val steel: SmeltingButtonSet get() = all.steel
    val gold: SmeltingButtonSet get() = all.gold
    val mithril: SmeltingButtonSet get() = all.mithril
    val adamant: SmeltingButtonSet get() = all.adamant
    val rune: SmeltingButtonSet get() = all.rune
}

data class SmeltingRecipe(
    val barId: Int,
    val levelRequired: Int,
    val experience: Int,
    val oreRequirements: List<OreRequirement>,
    val successChancePercent: Int = 100,
    val failureMessage: String? = null,
)

data class SmeltingRequest(
    val recipe: SmeltingRecipe,
    val amount: Int,
)

data class SmeltingSelection(
    val recipe: SmeltingRecipe,
    val amount: Int,
)

data class SmithingDisplayItem(
    val itemId: Int,
    val amount: Int,
)

class SmeltingRecipeBuilder internal constructor(
    private val barId: Int,
) {
    private var levelRequired: Int = 1
    private var experience: Int = 0
    private var successChancePercent: Int = 100
    private var failureMessage: String? = null
    private val ores = ArrayList<OreRequirement>()

    fun level(level: Int) {
        levelRequired = level
    }

    fun xp(experience: Int) {
        this.experience = experience
    }

    fun ore(itemId: Int, amount: Int = 1) {
        ores += OreRequirement(itemId, amount)
    }

    fun successChance(percent: Int) {
        successChancePercent = percent
    }

    fun failureMessage(message: String) {
        failureMessage = message
    }

    fun build(): SmeltingRecipe {
        return SmeltingRecipe(
            barId = barId,
            levelRequired = levelRequired,
            experience = experience,
            oreRequirements = ores.toList(),
            successChancePercent = successChancePercent,
            failureMessage = failureMessage,
        )
    }
}

class SmeltingRecipesBuilder {
    private val recipes = ArrayList<SmeltingRecipe>()

    fun bar(barId: Int, block: SmeltingRecipeBuilder.() -> Unit) {
        val builder = SmeltingRecipeBuilder(barId)
        builder.block()
        recipes += builder.build()
    }

    fun build(): List<SmeltingRecipe> = recipes
}

fun smeltingRecipes(block: SmeltingRecipesBuilder.() -> Unit): List<SmeltingRecipe> {
    val builder = SmeltingRecipesBuilder()
    builder.block()
    return builder.build()
}

object SmithingFrameDefinitions {
    @JvmField
    val smithingFrame: Array<Array<SmithingFrameEntry>> = arrayOf(
        arrayOf(
            entry(1205, 1, 1, 1, 1125, 1094), entry(1351, 1, 1, 1, 1126, 1091), entry(1422, 1, 2, 1, 1129, 1093),
            entry(1139, 1, 3, 1, 1127, 1102), entry(1277, 1, 3, 1, 1128, 1085), entry(819, 10, 4, 1, 1124, 1107),
            entry(4819, 15, 4, 1, 13357, 13358), entry(39, 15, 5, 1, 1130, 1108), entry(1321, 1, 5, 2, 1116, 1087),
            entry(1291, 1, 6, 2, 1089, 1086), entry(1155, 1, 7, 2, 1113, 1103), entry(864, 5, 7, 1, 1131, 1106),
            entry(1173, 1, 8, 2, 1114, 1104), entry(1337, 1, 9, 3, 1118, 1083), entry(1375, 1, 10, 3, 1095, 1092),
            entry(1103, 1, 11, 3, 1109, 1098), entry(1189, 1, 12, 3, 1115, 1105), entry(3095, 1, 13, 2, 8428, 8429),
            entry(1307, 1, 14, 3, 1090, 1088), entry(1087, 1, 16, 3, 1111, 1100), entry(1075, 1, 16, 3, 1110, 1099),
            entry(1117, 1, 18, 5, 1112, 1101), entry(1794, 1, 4, 1, 1132, 1096),
        ),
        arrayOf(
            entry(1203, 1, 15, 1, 1125, 1094), entry(1349, 1, 16, 1, 1126, 1091), entry(1420, 1, 17, 1, 1129, 1093),
            entry(1137, 1, 18, 1, 1127, 1102), entry(1279, 1, 19, 1, 1128, 1085), entry(820, 10, 19, 1, 1124, 1107),
            entry(4820, 15, 19, 1, 13357, 13358), entry(40, 15, 20, 1, 1130, 1108), entry(1323, 1, 20, 2, 1116, 1087),
            entry(1293, 1, 21, 2, 1089, 1086), entry(1153, 1, 22, 2, 1113, 1103), entry(863, 5, 22, 1, 1131, 1106),
            entry(1175, 1, 23, 2, 1114, 1104), entry(1335, 1, 24, 3, 1118, 1083), entry(1363, 1, 25, 3, 1095, 1092),
            entry(1101, 1, 26, 3, 1109, 1098), entry(1191, 1, 27, 3, 1115, 1105), entry(3096, 1, 28, 2, 8428, 8429),
            entry(1309, 1, 29, 3, 1090, 1088), entry(1081, 1, 31, 3, 1111, 1100), entry(1067, 1, 31, 3, 1110, 1099),
            entry(1115, 1, 33, 5, 1112, 1101), entry(4540, 1, 26, 1, 11459, 11461),
        ),
        arrayOf(
            entry(1207, 1, 30, 1, 1125, 1094), entry(1353, 1, 31, 1, 1126, 1091), entry(1424, 1, 32, 1, 1129, 1093),
            entry(1141, 1, 33, 1, 1127, 1102), entry(1281, 1, 34, 1, 1128, 1085), entry(821, 10, 34, 1, 1124, 1107),
            entry(1539, 15, 34, 1, 13357, 13358), entry(41, 15, 35, 1, 1130, 1108), entry(1325, 1, 35, 2, 1116, 1087),
            entry(1295, 1, 36, 2, 1089, 1086), entry(1157, 1, 37, 2, 1113, 1103), entry(865, 5, 37, 1, 1131, 1106),
            entry(1177, 1, 38, 2, 1114, 1104), entry(1339, 1, 39, 3, 1118, 1083), entry(1365, 1, 40, 3, 1095, 1092),
            entry(1105, 1, 41, 3, 1109, 1098), entry(1193, 1, 42, 3, 1115, 1105), entry(3097, 1, 43, 2, 8428, 8429),
            entry(1311, 1, 44, 3, 1090, 1088), entry(1083, 1, 46, 3, 1111, 1100), entry(1069, 1, 46, 3, 1110, 1099),
            entry(1119, 1, 48, 5, 1112, 1101), entry(4544, 1, 49, 1, 11459, 11461), entry(2370, 1, 36, 1, 1135, 1134),
        ),
        arrayOf(
            entry(1209, 1, 50, 1, 1125, 1094), entry(1355, 1, 51, 1, 1126, 1091), entry(1428, 1, 52, 1, 1129, 1093),
            entry(1143, 1, 53, 1, 1127, 1102), entry(1285, 1, 53, 1, 1128, 1085), entry(822, 10, 54, 1, 1124, 1107),
            entry(4822, 15, 54, 1, 13357, 13358), entry(42, 15, 55, 1, 1130, 1108), entry(1329, 1, 55, 2, 1116, 1087),
            entry(1299, 1, 56, 2, 1089, 1086), entry(1159, 1, 57, 2, 1113, 1103), entry(866, 5, 57, 1, 1131, 1106),
            entry(1181, 1, 58, 2, 1114, 1104), entry(1343, 1, 59, 3, 1118, 1083), entry(1369, 1, 60, 3, 1095, 1092),
            entry(1109, 1, 61, 3, 1109, 1098), entry(1197, 1, 62, 3, 1115, 1105), entry(3099, 1, 63, 2, 8428, 8429),
            entry(1315, 1, 64, 3, 1090, 1088), entry(1085, 1, 66, 3, 1111, 1100), entry(1071, 1, 66, 3, 1110, 1099),
            entry(1121, 1, 68, 5, 1112, 1101),
        ),
        arrayOf(
            entry(1211, 1, 70, 1, 1125, 1094), entry(1357, 1, 71, 1, 1126, 1091), entry(1430, 1, 72, 1, 1129, 1093),
            entry(1145, 1, 73, 1, 1127, 1102), entry(1287, 1, 74, 1, 1128, 1085), entry(823, 10, 74, 1, 1124, 1107),
            entry(4823, 15, 74, 1, 13357, 13358), entry(43, 15, 75, 1, 1130, 1108), entry(1331, 1, 75, 2, 1116, 1087),
            entry(1301, 1, 76, 2, 1089, 1086), entry(1161, 1, 77, 2, 1113, 1103), entry(867, 5, 77, 1, 1131, 1106),
            entry(1183, 1, 78, 2, 1114, 1104), entry(1345, 1, 79, 3, 1118, 1083), entry(1371, 1, 80, 3, 1095, 1092),
            entry(1111, 1, 81, 3, 1109, 1098), entry(1199, 1, 82, 3, 1115, 1105), entry(3100, 1, 83, 2, 8428, 8429),
            entry(1317, 1, 84, 3, 1090, 1088), entry(1091, 1, 86, 3, 1111, 1100), entry(1073, 1, 86, 3, 1110, 1099),
            entry(1123, 1, 88, 5, 1112, 1101),
        ),
        arrayOf(
            entry(1213, 1, 85, 1, 1125, 1094), entry(1359, 1, 86, 1, 1126, 1091), entry(1432, 1, 87, 1, 1129, 1093),
            entry(1147, 1, 88, 1, 1127, 1102), entry(1289, 1, 89, 1, 1128, 1085), entry(824, 10, 89, 1, 1124, 1107),
            entry(4824, 15, 89, 1, 13357, 13358), entry(44, 15, 90, 1, 1130, 1108), entry(1333, 1, 90, 2, 1116, 1087),
            entry(1303, 1, 91, 2, 1089, 1086), entry(1163, 1, 92, 2, 1113, 1103), entry(868, 5, 92, 1, 1131, 1106),
            entry(1185, 1, 93, 2, 1114, 1104), entry(1347, 1, 94, 3, 1118, 1083), entry(1373, 1, 95, 3, 1095, 1092),
            entry(1113, 1, 96, 3, 1109, 1098), entry(1201, 1, 97, 3, 1115, 1105), entry(3101, 1, 98, 2, 8428, 8429),
            entry(1319, 1, 99, 3, 1090, 1088), entry(1093, 1, 99, 3, 1111, 1100), entry(1079, 1, 99, 3, 1110, 1099),
            entry(1127, 1, 99, 5, 1112, 1101),
        ),
    )

    private fun entry(
        itemId: Int,
        outputAmount: Int,
        levelRequired: Int,
        barsRequired: Int,
        barCountLineId: Int,
        itemNameLineId: Int,
    ) = SmithingFrameEntry(
        itemId = itemId,
        outputAmount = outputAmount,
        levelRequired = levelRequired,
        barsRequired = barsRequired,
        barCountLineId = barCountLineId,
        itemNameLineId = itemNameLineId,
    )
}

data class SmithingFrameEntry(
    val itemId: Int,
    val outputAmount: Int,
    val levelRequired: Int,
    val barsRequired: Int,
    val barCountLineId: Int,
    val itemNameLineId: Int,
)

data class SmithingRequest(
    val tierId: Int,
    val product: SmithingProduct,
    val amount: Int,
    val barId: Int,
    val anvilX: Int,
    val anvilY: Int,
)

data class ActiveSmithingSelection(
    val tierId: Int,
    val barId: Int,
    val anvilX: Int,
    val anvilY: Int,
)

data class SmithingProduct(
    val itemId: Int,
    val outputAmount: Int,
    val levelRequired: Int,
    val barsRequired: Int,
    val barCountLineId: Int,
    val itemNameLineId: Int,
)

data class SmithingTier(
    val typeId: Int,
    val displayName: String,
    val barId: Int,
    val products: List<SmithingProduct>,
)
