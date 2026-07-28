package net.dodian.uber.skills.farming

import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.skills.SkillItemOnObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.SkillPluginDefinition
import net.dodian.uber.game.api.plugin.skills.manifest
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.api.SkillModuleDescriptor
import net.dodian.uber.game.api.plugin.runtime.TomlRecordReader

data class FarmingCropDef(
    val type: String,
    val name: String,
    val level: Int,
    val seedId: Int,
    val plantXp: Int,
    val harvestXp: Int,
    val harvestItem: Int,
)

object FarmingModule : SkillPlugin {
    val descriptor = SkillModuleDescriptor("skill.farming", "Farming")
    val crops: List<FarmingCropDef> by lazy { loadCrops() }

    /** Revision-218 empty patch roots for the crop types currently represented in patches.toml. */
    val patchObjectIds = intArrayOf(8573, 7840, 8132)
    private val seedIds by lazy { crops.map { it.seedId }.toIntArray() }

    override val definition: SkillPluginDefinition = skillPlugin("Farming", Skill.FARMING) {
        objectClick(preset = PolicyPreset.GATHERING, option = 2, *patchObjectIds) { interaction ->
            inspectPatch(interaction)
        }
        itemOnObject(PolicyPreset.PRODUCTION, *patchObjectIds, itemIds = seedIds) { interaction ->
            plantSeed(interaction)
        }
    }

    override val contentManifest: ContentModuleManifest = definition.manifest(
        id = descriptor.id,
        owner = "gameplay",
        version = descriptor.version,
        maturity = ContentMaturity.STABLE,
    )

    private fun inspectPatch(interaction: SkillObjectInteraction): Boolean {
        val player = interaction.player
        player.ui.message("You inspect the farming patch.")
        return true
    }

    private fun plantSeed(interaction: SkillItemOnObjectInteraction): Boolean {
        val player = interaction.player
        val crop = crops.firstOrNull { it.seedId == interaction.itemId } ?: return false
        if (player.skills.current(Skill.FARMING) < crop.level) {
            player.ui.message("You need level ${crop.level} farming to plant this seed.")
            return true
        }
        if (player.inventory.transaction { remove(crop.seedId, 1) }) {
            player.actions.animate(2291)
            player.skills.gainXp(crop.plantXp, Skill.FARMING)
            player.actions.triggerRandomEvent(crop.plantXp)
            player.ui.message("You plant the ${crop.name} seed.")
        }
        return true
    }

    private fun loadCrops(): List<FarmingCropDef> =
        TomlRecordReader.readRecords("farming/patches.toml", "crop").map { row ->
            FarmingCropDef(
                type = row.getValue("type"),
                name = row.getValue("name"),
                level = row.getValue("level").toInt(),
                seedId = row.getValue("seed_id").toInt(),
                plantXp = row.getValue("plant_xp").toInt(),
                harvestXp = row.getValue("harvest_xp").toInt(),
                harvestItem = row.getValue("harvest_item").toInt(),
            )
        }
}
