package net.dodian.uber.skills.crafting

import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.skills.SkillItemOnItemInteraction
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.SkillPluginDefinition
import net.dodian.uber.game.api.plugin.skills.manifest
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.api.SkillModuleDescriptor
import net.dodian.uber.skills.runtime.TomlRecordReader

data class CraftingGemDef(
    val uncutId: Int,
    val cutId: Int,
    val requiredLevel: Int,
    val experience: Int,
    val animationId: Int,
)

data class CraftingHideDef(
    val itemId: Int,
    val experience: Int,
    val glovesId: Int,
    val glovesLevel: Int,
    val chapsId: Int,
    val chapsLevel: Int,
    val bodyId: Int,
    val bodyLevel: Int,
)

object CraftingModule : SkillPlugin {
    val descriptor = SkillModuleDescriptor("skill.crafting", "Crafting")
    val gems: List<CraftingGemDef> by lazy { loadGems() }
    val hides: List<CraftingHideDef> by lazy { loadHides() }

    private const val CHISEL_ITEM_ID = 1755
    private const val NEEDLE_ITEM_ID = 1733

    override val definition: SkillPluginDefinition = skillPlugin("Crafting", Skill.CRAFTING) {
        gems.forEach { gem ->
            itemOnItem(PolicyPreset.PRODUCTION, CHISEL_ITEM_ID, gem.uncutId) { cutGem(it, gem) }
        }
        hides.forEach { hide ->
            itemOnItem(PolicyPreset.PRODUCTION, NEEDLE_ITEM_ID, hide.itemId) { craftHide(it, hide) }
        }
    }

    override val contentManifest: ContentModuleManifest = definition.manifest(
        id = descriptor.id,
        owner = "gameplay",
        version = descriptor.version,
        maturity = ContentMaturity.STABLE,
    )

    private fun cutGem(interaction: SkillItemOnItemInteraction, gem: CraftingGemDef): Boolean {
        val player = interaction.player
        if (player.skills.current(Skill.CRAFTING) < gem.requiredLevel) {
            player.ui.message("You need a crafting level of ${gem.requiredLevel} to cut this gem.")
            return true
        }
        if (player.inventory.transaction { remove(gem.uncutId, 1); add(gem.cutId, 1) }) {
            player.actions.animate(gem.animationId)
            player.skills.gainXp(gem.experience, Skill.CRAFTING)
            player.actions.triggerRandomEvent(gem.experience)
            player.ui.message("You cut the ${player.inventory.itemName(gem.uncutId).lowercase()}.")
        }
        return true
    }

    private fun craftHide(interaction: SkillItemOnItemInteraction, hide: CraftingHideDef): Boolean {
        val player = interaction.player
        if (player.skills.current(Skill.CRAFTING) < hide.glovesLevel) {
            player.ui.message("You need level ${hide.glovesLevel} crafting to craft from this leather.")
            return true
        }
        return true
    }

    private fun loadGems(): List<CraftingGemDef> =
        TomlRecordReader.readRecords("crafting/recipes.toml", "gem").map { row ->
            CraftingGemDef(
                uncutId = row.getValue("uncut_id").toInt(),
                cutId = row.getValue("cut_id").toInt(),
                requiredLevel = row.getValue("required_level").toInt(),
                experience = row.getValue("experience").toInt(),
                animationId = row.getValue("animation_id").toInt(),
            )
        }

    private fun loadHides(): List<CraftingHideDef> =
        TomlRecordReader.readRecords("crafting/recipes.toml", "hide").map { row ->
            CraftingHideDef(
                itemId = row.getValue("item_id").toInt(),
                experience = row.getValue("experience").toInt(),
                glovesId = row.getValue("gloves_id").toInt(),
                glovesLevel = row.getValue("gloves_level").toInt(),
                chapsId = row.getValue("chaps_id").toInt(),
                chapsLevel = row.getValue("chaps_level").toInt(),
                bodyId = row.getValue("body_id").toInt(),
                bodyLevel = row.getValue("body_level").toInt(),
            )
        }
}
