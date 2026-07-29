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

object SmithingModule : SkillPlugin {
    val descriptor = SkillModuleDescriptor("skill.smithing", "Smithing")
    val smeltingRecipes: List<SmeltingRecipe> get() = SmeltingRegistry.recipes

    /** Verified live-cache furnace IDs. Do not resolve gameplay routes through RSCM. */
    val furnaceObjectIds = intArrayOf(2030, 3994, 16469)
    private val smeltingInterfaceFurnaces = intArrayOf(2030, 16469)
    val anvilObjectIds = intArrayOf(2097)

    override val definition: SkillPluginDefinition = skillPlugin("Smithing", Skill.SMITHING) {
        itemOnObject(PolicyPreset.PRODUCTION, *furnaceObjectIds) { interaction ->
            handleFurnace(interaction)
        }
        itemOnObject(PolicyPreset.PRODUCTION, *anvilObjectIds) { interaction ->
            handleAnvil(interaction)
        }
        // Revision-218 exposes Smelt as option two. Small furnace 3994 has no cache
        // click option, but remains a valid item-on-object furnace.
        objectClick(preset = PolicyPreset.PRODUCTION, option = 2, *smeltingInterfaceFurnaces) { interaction ->
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
        val recipe = smeltingRecipes.firstOrNull { recipe -> recipe.oreRequirements.any { it.itemId == interaction.itemId } } ?: return false
        smeltOnce(player, recipe)
        return true
    }

    private fun smeltOnce(player: SkillPlayer, recipe: SmeltingRecipe): Boolean {
        if (player.skills.current(Skill.SMITHING) < recipe.levelRequired) {
            player.ui.message("You need level ${recipe.levelRequired} smithing to smelt this bar.")
            return false
        }
        val committed = player.inventory.transaction {
            recipe.oreRequirements.forEach { requirement -> remove(requirement.itemId, requirement.amount) }
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
            .filter { recipe -> recipe.oreRequirements.any { player.inventory.amount(it.itemId) > 0 } }
            .take(3)
        if (candidates.isEmpty()) {
            player.ui.message("You don't have any ore to smelt.")
            return true
        }
        fun recipeKey(recipe: SmeltingRecipe) = "smithing.recipe.${recipe.barId}"
        val entries = candidates.map { recipe ->
            val skillRecipe = skillRecipe(recipeKey(recipe), recipe.barId) {
                recipe.oreRequirements.forEach { requirement -> material(requirement.itemId, requirement.amount) }
                requirement(recipe.levelRequired)
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
        if (SmeltingRegistry.findRecipe(interaction.itemId) == null) return false
        SmithingAnvilService.openForBar(
            interaction.player,
            interaction.itemId,
            interaction.position.x,
            interaction.position.y,
        )
        return true
    }
}
