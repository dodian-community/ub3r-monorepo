package net.dodian.uber.skills.smithing

import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.skills.SkillItemOnObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillPlayer
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.SkillPluginDefinition
import net.dodian.uber.game.api.plugin.skills.manifest
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.api.SkillModuleDescriptor
import net.dodian.uber.skills.api.SkillMultiAction
import net.dodian.uber.skills.api.SkillMultiConfig
import net.dodian.uber.skills.api.SkillMultiEntry
import net.dodian.uber.skills.api.SkillMultiLayout
import net.dodian.uber.skills.api.skillRecipe
import net.dodian.uber.skills.runtime.TomlRecordReader

data class SmeltingRecipeDef(
    val barId: Int,
    val level: Int,
    val experience: Int,
    val ores: List<Int>,
    val oreAmounts: List<Int>,
)

object SmithingModule : SkillPlugin {
    val descriptor = SkillModuleDescriptor("skill.smithing", "Smithing")
    val smeltingRecipes: List<SmeltingRecipeDef> by lazy { loadSmeltingRecipes() }

    private val furnaceObjectIds = intArrayOf(
        2030, 2150, 2151, 2152, 2153, 2966, 3294, 3994, 4304, 6189, 6190, 10082,
        11009, 11010, 11666, 12100, 12655, 12809, 16469, 16657, 18525, 18526,
        21879, 22721, 24009, 26300, 26814, 28565, 29662, 30157, 30158
    )
    private val anvilObjectIds = intArrayOf(2097, 2783, 4306, 6150, 26813)

    override val definition: SkillPluginDefinition = skillPlugin("Smithing", Skill.SMITHING) {
        itemOnObject(PolicyPreset.PRODUCTION, *furnaceObjectIds) { interaction ->
            handleFurnace(interaction)
        }
        itemOnObject(PolicyPreset.PRODUCTION, *anvilObjectIds) { interaction ->
            handleAnvil(interaction)
        }
        // Clicking the furnace directly opens a selection of all bars available to smelt.
        objectClick(preset = PolicyPreset.PRODUCTION, option = 1, *furnaceObjectIds) { interaction ->
            openSmeltingPicker(interaction)
        }
    }

    override val contentManifest: ContentModuleManifest = definition.manifest(
        id = descriptor.id,
        owner = "gameplay",
        version = descriptor.version,
        maturity = ContentMaturity.STABLE,
    )

    private fun handleFurnace(interaction: SkillItemOnObjectInteraction): Boolean {
        val player = interaction.player
        val recipe = smeltingRecipes.firstOrNull { it.ores.contains(interaction.itemId) } ?: return false
        smeltOnce(player, recipe)
        return true
    }

    private fun smeltOnce(player: SkillPlayer, recipe: SmeltingRecipeDef): Boolean {
        if (player.skills.current(Skill.SMITHING) < recipe.level) {
            player.ui.message("You need level ${recipe.level} smithing to smelt this bar.")
            return false
        }
        val committed = player.inventory.transaction {
            recipe.ores.forEachIndexed { index, oreId -> remove(oreId, recipe.oreAmounts.getOrElse(index) { 1 }) }
            add(recipe.barId, 1)
        }
        if (!committed) {
            player.ui.message("You don't have the ores required to smelt this bar.")
            return false
        }
        player.skills.gainXp(recipe.experience, Skill.SMITHING)
        player.actions.triggerRandomEvent(recipe.experience)
        player.ui.message("You smelt the ${player.inventory.itemName(recipe.barId).lowercase()}.")
        return true
    }

    private fun openSmeltingPicker(interaction: SkillObjectInteraction): Boolean {
        val player = interaction.player
        val candidates = smeltingRecipes
            .filter { recipe -> recipe.ores.any { player.inventory.amount(it) > 0 } }
            .take(3)
        if (candidates.isEmpty()) {
            player.ui.message("You don't have any ore to smelt.")
            return true
        }
        fun recipeKey(recipe: SmeltingRecipeDef) = "smithing.recipe.${recipe.barId}"
        val entries = candidates.map { recipe ->
            val skillRecipe = skillRecipe(recipeKey(recipe), recipe.barId) {
                recipe.ores.forEachIndexed { index, oreId -> material(oreId, recipe.oreAmounts.getOrElse(index) { 1 }) }
                requirement(recipe.level)
                experience(recipe.experience)
            }
            SkillMultiEntry(skillRecipe)
        }
        val config = SkillMultiConfig(
            key = "smithing.smelt",
            verb = "smelt",
            action = SkillMultiAction.SMELT,
            presentationKey = "makeall",
            layout = SkillMultiLayout.SPECIALIZED,
            entries = entries,
        )
        return player.production.open(config) { selection ->
            val recipe = candidates.firstOrNull { recipeKey(it) == selection.recipeKey }
            if (recipe != null) repeat(selection.amount) { smeltOnce(player, recipe) }
        }
    }

    private fun handleAnvil(interaction: SkillItemOnObjectInteraction): Boolean {
        val player = interaction.player
        val recipe = smeltingRecipes.firstOrNull { it.barId == interaction.itemId } ?: return false
        if (player.skills.current(Skill.SMITHING) < recipe.level) {
            player.ui.message("You need level ${recipe.level} smithing to smith this bar.")
            return true
        }
        return true
    }

    private fun loadSmeltingRecipes(): List<SmeltingRecipeDef> =
        TomlRecordReader.readRecords("smithing/recipes.toml", "smelt").map { row ->
            val oresStr = row.getValue("ores").removeSurrounding("[", "]").split(",")
            val oreAmountsStr = row.getValue("ore_amounts").removeSurrounding("[", "]").split(",")
            SmeltingRecipeDef(
                barId = row.getValue("bar_id").toInt(),
                level = row.getValue("level").toInt(),
                experience = row.getValue("experience").toInt(),
                ores = oresStr.map { it.trim().toInt() },
                oreAmounts = oreAmountsStr.map { it.trim().toInt() },
            )
        }
}
