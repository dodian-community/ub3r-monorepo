package net.dodian.uber.game.content.skills.smithing

object SmithingDefinitions {
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
