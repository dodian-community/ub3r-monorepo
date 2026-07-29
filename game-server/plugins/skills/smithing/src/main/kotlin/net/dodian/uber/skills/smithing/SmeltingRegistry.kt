package net.dodian.uber.skills.smithing

import net.dodian.uber.game.api.plugin.runtime.TomlRecordReader

data class OreRequirement(val itemId: Int, val amount: Int)

data class SmeltingRecipe(
    val barId: Int,
    val levelRequired: Int,
    val experience: Int,
    val oreRequirements: List<OreRequirement>,
    val successChancePercent: Int = 100,
    val failureMessage: String? = null,
)

data class SmeltingSelection(val recipe: SmeltingRecipe, val amount: Int)
data class FurnaceButtonMapping(val buttonId: Int, val barId: Int, val amount: Int)

/** Immutable, plugin-owned source for legacy furnace and Superheat bar rules. */
object SmeltingRegistry {
    private val furnaceFrameIds = intArrayOf(2405, 2406, 2407, 2409, 2410, 2411, 2412, 2413)
    val recipes: List<SmeltingRecipe> by lazy {
        TomlRecordReader.readRecords("smithing/recipes.toml", "smelt").map { row ->
            val ores = row.getValue("ores").removeSurrounding("[", "]").split(",").map { it.trim().toInt() }
            val amounts = row.getValue("ore_amounts").removeSurrounding("[", "]").split(",").map { it.trim().toInt() }
            SmeltingRecipe(
                barId = row.getValue("bar_id").toInt(),
                levelRequired = row.getValue("level").toInt(),
                experience = row.getValue("experience").toInt(),
                oreRequirements = ores.mapIndexed { index, ore -> OreRequirement(ore, amounts.getOrElse(index) { 1 }) },
                successChancePercent = row["success_chance"]?.toInt() ?: 100,
                failureMessage = row["failure_message"],
            )
        }
    }
    private val classicButtonIds = intArrayOf(
        15147,15146,10247,9110,15151,15150,15149,15148,15155,15154,15153,15152,
        15159,15158,15157,15156,15163,15162,15161,15160,29017,29016,24253,16062,
        29022,29020,29019,29018,29026,29025,29024,29023,
    )
    val furnaceButtonMappings: List<FurnaceButtonMapping> by lazy {
        classicButtonIds.mapIndexed { index, buttonId ->
            FurnaceButtonMapping(buttonId, recipes[index / 4].barId, intArrayOf(1, 5, 10, 28)[index % 4])
        }
    }

    @JvmStatic fun findRecipe(barId: Int): SmeltingRecipe? = recipes.firstOrNull { it.barId == barId }
    @JvmStatic fun findRecipeByOre(itemId: Int): SmeltingRecipe? = recipes.firstOrNull { recipe -> recipe.oreRequirements.any { it.itemId == itemId } }
    @JvmStatic fun mapping(buttonId: Int): FurnaceButtonMapping? = furnaceButtonMappings.firstOrNull { it.buttonId == buttonId }
    @JvmStatic fun isFurnaceFrame(id: Int): Boolean = furnaceFrameIds.contains(id)
    @JvmStatic fun isFurnaceButton(id: Int): Boolean = furnaceFrameIds.contains(id) || furnaceButtonMappings.any { it.buttonId == id }
    @JvmStatic fun furnaceFrames(): IntArray = furnaceFrameIds.copyOf()
}
