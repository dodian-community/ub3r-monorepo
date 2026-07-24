package net.dodian.uber.skills.smithing

import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.skills.SkillItemOnObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.SkillPluginDefinition
import net.dodian.uber.game.api.plugin.skills.manifest
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.api.SkillModuleDescriptor
import net.dodian.uber.skills.runtime.TomlRecordReader

data class SmeltingRecipeDef(
    val barId: Int,
    val level: Int,
    val experience: Int,
    val ores: List<Int>,
)

object SmithingModule : SkillPlugin {
    val descriptor = SkillModuleDescriptor("skill.smithing", "Smithing")
    val smeltingRecipes: List<SmeltingRecipeDef> by lazy { loadSmeltingRecipes() }

    private val furnaceObjectIds = intArrayOf(26814, 11666)
    private val anvilObjectIds = intArrayOf(2097, 4306)

    override val definition: SkillPluginDefinition = skillPlugin("Smithing", Skill.SMITHING) {
        itemOnObject(PolicyPreset.PRODUCTION, *furnaceObjectIds) { interaction ->
            handleFurnace(interaction)
        }
        itemOnObject(PolicyPreset.PRODUCTION, *anvilObjectIds) { interaction ->
            handleAnvil(interaction)
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
        if (player.skills.current(Skill.SMITHING) < recipe.level) {
            player.ui.message("You need level ${recipe.level} smithing to smelt this bar.")
            return true
        }
        return true
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
            SmeltingRecipeDef(
                barId = row.getValue("bar_id").toInt(),
                level = row.getValue("level").toInt(),
                experience = row.getValue("experience").toInt(),
                ores = oresStr.map { it.trim().toInt() },
            )
        }
}
