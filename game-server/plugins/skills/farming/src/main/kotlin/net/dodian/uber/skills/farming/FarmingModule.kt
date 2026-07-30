package net.dodian.uber.skills.farming

import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
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

    /**
     * Empty-patch ids for the "Allotment"/"Flower Patch"/"Herb patch" generic types (confirmed
     * op2=Inspect, op4=Guide against the rev-218 loc cache). These do not appear in any of the 8
     * currently-coded patch locations below - pre-existing gap, not introduced by this migration.
     */
    val patchObjectIds = intArrayOf(8573, 7840, 8132)

    /**
     * Static placed ids of the 8 currently-coded allotment/flower/herb patch locations (Catherby,
     * Ardougne, Canifis, Gnome Stronghold, Taverley - 27/44 crop types). Multiloc parents: the
     * client swaps the rendered child model/menu per growth stage, but click packets always
     * report this placed id (verified: old Farming.kt's clickPatch/interactItemBin already key
     * off these exact ids). Bush/fruit-tree/tree patch locations (17 remaining crop types) have no
     * confirmed real-world coordinates in this repo yet and are intentionally not wired here.
     */
    val livePatchObjectIds = intArrayOf(
        7580, 7848, 7849, 7850, 7962, 7965, 8151, 8152, 8153, 8388, 8552, 8553, 8554, 8555, 8556, 8557, 19147,
    )

    /** Catherby/Ardougne/Canifis compost bins (op1=Open, op5=Dump) plus Falador's (item-on-object only, no menu ops). */
    val compostBinObjectIds = intArrayOf(7837, 7839, 7838, 1003)

    private const val RAKE = 5341
    private const val SPADE = 952
    private const val SECATEURS = 5329
    private const val MAGIC_SECATEURS = 7409
    private const val PLANT_CURE = 6036
    private const val BUCKET = 1925
    private const val VOLCANIC_ASH = 21622
    private val wateringCanIds = intArrayOf(5331, 5333, 5334, 5335, 5336, 5337, 5338, 5339, 5340)
    private val regularCompostItemIds = intArrayOf(
        6055, 6010, 6014, 6020, 1793, 5986, 5504, 1955, 1963, 2108, 5970,
        1957, 1942, 1965, 1951, 2126, 753, 1779, 401, 249, 199, 251, 201, 253, 203, 255, 205, 257, 207,
    )
    private val superCompostItemIds = intArrayOf(
        2114, 5982, 5972, 5974, 5978, 5976, 231, 247, 239, 6018, 2998, 3049,
        261, 211, 263, 213, 3000, 3051, 265, 215, 2481, 2485, 267, 217, 269, 219, 259, 209,
    )

    private val seedIds by lazy { crops.map { it.seedId }.toIntArray() }
    private val patchItemIds by lazy {
        (seedIds.toList() + RAKE + SPADE + SECATEURS + MAGIC_SECATEURS + PLANT_CURE +
            wateringCanIds.toList() + regularCompostItemIds.toList() + superCompostItemIds.toList())
            .distinct().toIntArray()
    }
    private val binItemIds by lazy {
        (listOf(BUCKET, VOLCANIC_ASH) + regularCompostItemIds.toList() + superCompostItemIds.toList())
            .distinct().toIntArray()
    }

    override val definition: SkillPluginDefinition = skillPlugin("Farming", Skill.FARMING) {
        objectClick(preset = PolicyPreset.GATHERING, option = 2, *patchObjectIds) { interaction ->
            interaction.player.farming.inspectPatch(interaction.objectId)
            true
        }
        // Assumption (flagged for live-client verification): op1 is the primary state-driven
        // action slot for these patches; clickPatch() itself derives the correct behavior from
        // current patch state, so a wrong option number here would be a no-op, not a data risk.
        objectClick(preset = PolicyPreset.GATHERING, option = 1, *livePatchObjectIds) { interaction ->
            interaction.player.farming.clickPatch(interaction.objectId)
        }
        objectClick(preset = PolicyPreset.GATHERING, option = 1, *compostBinObjectIds) { interaction ->
            interaction.player.farming.interactBin(interaction.objectId, 1)
            true
        }
        objectClick(preset = PolicyPreset.GATHERING, option = 5, *compostBinObjectIds) { interaction ->
            interaction.player.farming.interactBin(interaction.objectId, 5)
            true
        }
        itemOnObject(PolicyPreset.PRODUCTION, *livePatchObjectIds, *patchObjectIds, itemIds = patchItemIds) { interaction ->
            interaction.player.farming.useItem(interaction.objectId, interaction.itemId)
        }
        itemOnObject(PolicyPreset.PRODUCTION, *compostBinObjectIds, itemIds = binItemIds) { interaction ->
            interaction.player.farming.useItem(interaction.objectId, interaction.itemId)
        }
    }

    override val contentManifest: ContentModuleManifest = definition.manifest(
        id = descriptor.id,
        owner = "gameplay",
        version = descriptor.version,
        maturity = ContentMaturity.STABLE,
    )

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
