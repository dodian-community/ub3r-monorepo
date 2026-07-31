package net.dodian.uber.skills.farming

import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.skills.SkillPlayer
import net.dodian.uber.game.api.plugin.skills.SkillPatchSlot
import net.dodian.uber.game.api.plugin.skills.SkillCompostBinState
import net.dodian.uber.game.api.plugin.skills.SkillDialogueOption
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.SkillPluginDefinition
import net.dodian.uber.game.api.plugin.skills.manifest
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.api.SkillModuleDescriptor
import net.dodian.uber.game.api.plugin.runtime.TomlRecordReader
import kotlin.math.roundToInt

data class FarmingCropDef(
    val type: String,
    val name: String,
    val level: Int,
    val seedId: Int,
    val plantXp: Int,
    val harvestXp: Int,
    val harvestItem: Int,
    val stages: Int,
    val growTick: Int,
    val diseaseChance: Int,
    val checkHealthXp: Int,
)

/** Where a live patch object id sits: which persisted-state slot it maps to, and its crop type. */
private data class PatchLocation(val patchName: String, val slot: Int, val patchType: String)

object FarmingModule : SkillPlugin {
    val descriptor = SkillModuleDescriptor("skill.farming", "Farming")
    val crops: List<FarmingCropDef> by lazy { loadCrops() }

    /**
     * Empty-patch ids for the "Allotment"/"Flower Patch"/"Herb patch" generic types (confirmed
     * op2=Inspect, op4=Guide against the rev-218 loc cache). These do not appear in any of the
     * live patch locations below - pre-existing gap, not introduced by this migration.
     */
    val patchObjectIds = intArrayOf(8573, 7840, 8132)

    /**
     * objectId -> (patch location name, slot index, crop type) for every confirmed patch, across
     * all 6 categories. Allotment/flower/herb resolved by cross-referencing this server's own
     * cache-decoded object positions (`CacheCollisionAuditStore`) against the Tarnish reference
     * server's exact per-patch-type zone rectangles - `GameObjectData.name`/`.description` are
     * both the literal string `"null"` for every one of these ids (genuinely absent from the
     * cache, not a decoder bug), so there is no name-based way to resolve patch type; see the
     * migration plan/memory for the full cross-reference. Catherby/Ardougne/Canifis each got an
     * *exact* coordinate match for their 4-patch clusters (footprint rectangles matched Tarnish's
     * NORTH_ALLOTMENT_PATCH/SOUTH_ALLOTMENT_PATCH/FLOWER_PATCH/HERB_PATCH constants down to the
     * tile).
     *
     * The remaining single-id "standalone" locations (Taverley/Gnome Stronghold x2/Catherby East/
     * Ardougne South/N Falador/Varrock/Lumbridge/Tree Gnome Village/Brimhaven/Champions Guild/
     * Rimmington/Etceteria) are all bush/fruit-tree/tree, confirmed by cross-referencing this
     * server's cache-decoded positions against the 2009scape reference server's `FarmingPatch.kt`
     * object-id ranges (`tree`=8388-8391+19147, `fruit tree`=7962-7965, `bush`=7577-7580) - every
     * one of those 13 ids resolved to exactly one real-world position. **Correction to an earlier
     * best-effort guess**: `CATHERBY_EAST`(7965)/`GNOME_STRONGHOLD_EAST`(7962)/`ARDOUGNE_SOUTH`
     * (7580) were previously classified "herb" (a size-signature guess made before this
     * cross-reference existed) - they are not in the herb id range at all and are actually
     * fruit-tree/fruit-tree/bush respectively. `TAVERLY_SOUTH`(8388)/`GNOME_STRONGHOLD_SOUTH`
     * (19147) were previously excluded as "bush-sized, unclassifiable" - both are tree patches.
     * The `FarmingData.patches` enum constant names were NOT renamed to match (they're just
     * persisted-JSON key labels; renaming would silently orphan existing save data under the old
     * key), only the `patchType` here changed for these 5 ids.
     */
    private val patchLocations: Map<Int, PatchLocation> = buildMap {
        fun location(patchName: String, vararg entries: Pair<Int, String>) {
            entries.forEachIndexed { slot, (objectId, type) -> put(objectId, PatchLocation(patchName, slot, type)) }
        }
        location("CATHERBY_WEST", 8552 to "allotment", 8553 to "allotment", 7848 to "flower", 8151 to "herb")
        location("CATHERBY_EAST", 7965 to "fruit tree")
        location("ARDOUGNE_EAST", 8554 to "allotment", 8555 to "allotment", 7849 to "flower", 8152 to "herb")
        location("ARDOUGNE_SOUTH", 7580 to "bush")
        location("CANIFIS_NORTH", 8556 to "allotment", 8557 to "allotment", 7850 to "flower", 8153 to "herb")
        location("GNOME_STRONGHOLD_EAST", 7962 to "fruit tree")
        location("TAVERLY_SOUTH", 8388 to "tree")
        location("GNOME_STRONGHOLD_SOUTH", 19147 to "tree")
        location("N_FALADOR_TREE", 8389 to "tree")
        location("VARROCK_TREE", 8390 to "tree")
        location("LUMBRIDGE_TREE", 8391 to "tree")
        location("TREE_GNOME_VILLAGE_FRUIT_TREE", 7963 to "fruit tree")
        location("BRIMHAVEN_FRUIT_TREE", 7964 to "fruit tree")
        location("CHAMPIONS_GUILD_BUSH", 7577 to "bush")
        location("RIMMINGTON_BUSH", 7578 to "bush")
        location("ETCETERIA_BUSH", 7579 to "bush")
    }

    /** Static placed ids of every confirmed patch location across all 6 categories (25 ids
     * total: 12 allotment/flower/herb + 13 bush/fruit-tree/tree) - see [patchLocations]. Multiloc
     * parents: the client swaps the rendered child model/menu per growth stage, but click
     * packets always report this placed id (verified: old Farming.kt's clickPatch/
     * interactItemBin already keyed off these exact ids). */
    val livePatchObjectIds: IntArray = patchLocations.keys.toIntArray()

    /** Catherby/Ardougne/Canifis compost bins (op1=Open, op5=Dump) plus Falador's (item-on-object only, no menu ops). */
    val compostBinObjectIds = intArrayOf(7837, 7839, 7838, 1003)

    private const val RAKE = 5341
    private const val SPADE = 952
    private const val SECATEURS = 5329
    private const val MAGIC_SECATEURS = 7409
    private const val PLANT_CURE = 6036
    private const val BUCKET = 1925
    private const val VOLCANIC_ASH = 21622
    private const val SEED_DIBBER = 5343
    private const val EMPTY_PLANT_POT = 5350
    private val wateringCanIds = intArrayOf(5331, 5333, 5334, 5335, 5336, 5337, 5338, 5339, 5340)
    private val regularCompostItemIds = intArrayOf(
        6055, 6010, 6014, 6020, 1793, 5986, 5504, 1955, 1963, 2108, 5970,
        1957, 1942, 1965, 1951, 2126, 753, 1779, 401, 249, 199, 251, 201, 253, 203, 255, 205, 257, 207,
    )
    private val superCompostItemIds = intArrayOf(
        2114, 5982, 5972, 5974, 5978, 5976, 231, 247, 239, 6018, 2998, 3049,
        261, 211, 263, 213, 3000, 3051, 265, 215, 2481, 2485, 267, 217, 269, 219, 259, 209,
    )

    // Persisted patch state string constants - mirrors legacy FarmingData.patchState/.compost
    // 1:1 (kept as plain strings at the SkillFarmingState boundary, not a new shared enum).
    private const val WEED = "WEED"
    private const val GROWING = "GROWING"
    private const val PROTECTED = "PROTECTED"
    private const val DISEASE = "DISEASE"
    private const val DEAD = "DEAD"
    private const val HARVEST = "HARVEST"
    private const val WATER = "WATER"
    private const val PRODUCTION = "PRODUCTION"
    private const val STUMP = "STUMP"
    private const val NO_COMPOST = "NONE"
    private const val COMPOST_ITEM_ID = 6032
    private const val SUPERCOMPOST_ITEM_ID = 6034
    private const val ULTRACOMPOST_ITEM_ID = 21483

    private const val RAKE_ANIM = 2273
    private const val SPADE_ANIM = 830
    private const val WATERCAN_ANIM = 2293
    private const val PLANTSEED_ANIM = 2291
    private const val HARVEST_ANIM = 2282
    private const val COMPOST_PATCH_ANIM = 2283
    private const val CURING_ANIM = 2288
    private const val PRUNE_SECATEURS_ANIM = 2279
    private const val HARVEST_FRUIT_ANIM = 2280
    private const val HARVEST_BUSH_ANIM = 2281

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
            inspectPatch(interaction.player, interaction.objectId)
            true
        }
        // Assumption (flagged for live-client verification): op1 is the primary state-driven
        // action slot for these patches; clickPatch() itself derives the correct behavior from
        // current patch state, so a wrong option number here would be a no-op, not a data risk.
        objectClick(preset = PolicyPreset.GATHERING, option = 1, *livePatchObjectIds) { interaction ->
            clickPatch(interaction.player, interaction.objectId)
        }
        objectClick(preset = PolicyPreset.GATHERING, option = 2, *livePatchObjectIds) { interaction ->
            inspectPatch(interaction.player, interaction.objectId)
            true
        }
        objectClick(preset = PolicyPreset.GATHERING, option = 1, *compostBinObjectIds) { interaction ->
            interactBin(interaction.player, interaction.objectId, 1)
            true
        }
        objectClick(preset = PolicyPreset.GATHERING, option = 5, *compostBinObjectIds) { interaction ->
            interactBin(interaction.player, interaction.objectId, 5)
            true
        }
        itemOnObject(PolicyPreset.PRODUCTION, *livePatchObjectIds, *patchObjectIds, itemIds = patchItemIds) { interaction ->
            useItem(interaction.player, interaction.objectId, interaction.itemId)
        }
        itemOnObject(PolicyPreset.PRODUCTION, *compostBinObjectIds, itemIds = binItemIds) { interaction ->
            useItemOnBin(interaction.player, interaction.objectId, interaction.itemId)
        }
    }

    override val contentManifest: ContentModuleManifest = definition.manifest(
        id = descriptor.id,
        owner = "gameplay",
        version = descriptor.version,
        maturity = ContentMaturity.STABLE,
    )

    // --- Patch interactions: weed/plant/water/compost/disease/harvest across all 6 categories.
    // Reproduces legacy Farming.kt's clickPatch/interactItemBin/inspectPatch exactly. STUMP/
    // PRODUCTION and the "check health" second-harvest tier (checkHealthXp>0, gated per-crop via
    // FarmingCropDef.checkHealthXp) only ever fire for bush/fruit-tree/tree - allotment/flower/
    // herb crops all have checkHealthXp=0, so they always take the ordinary HARVEST/DISEASE
    // branches below and never reach the checkHealthXp-gated ones (Farming part 3).

    private fun slotFor(player: SkillPlayer, objectId: Int): Pair<PatchLocation, SkillPatchSlot>? {
        val loc = patchLocations[objectId] ?: return null
        val slot = player.farmingState.patchSlots().firstOrNull { it.patchName == loc.patchName && it.slot == loc.slot }
            ?: SkillPatchSlot(loc.patchName, loc.slot, -1, WEED, NO_COMPOST, 0, 0, -1)
        return loc to slot
    }

    private fun cropFor(itemId: Int) = crops.firstOrNull { it.seedId == itemId }

    private fun clearPatch(player: SkillPlayer, loc: PatchLocation) {
        player.farmingState.writePatchSlot(loc.patchName, loc.slot, -1, WEED, NO_COMPOST, 3, 0, -1)
        player.farmingState.markDirty()
        player.farmingState.notifyInteraction()
        player.farmingState.refreshVisuals()
    }

    fun clickPatch(player: SkillPlayer, objectId: Int): Boolean {
        val (loc, slot) = slotFor(player, objectId) ?: return false
        when {
            slot.state == WEED && slot.stageOrLife < 3 -> {
                if (player.inventory.contains(RAKE)) {
                    player.actions.animate(RAKE_ANIM)
                    player.inventory.transaction { add(regularCompostItemIds[0]) }
                    player.farmingState.writePatchSlot(loc.patchName, loc.slot, slot.itemId, WEED, slot.compost, slot.stageOrLife + 1, slot.progress, slot.plantedBy)
                    player.farmingState.markDirty()
                    player.farmingState.notifyInteraction()
                    player.farmingState.refreshVisuals()
                } else {
                    player.ui.message("You need a rake in order to clear the weed.")
                }
            }
            slot.state == DEAD -> {
                if (player.inventory.contains(SPADE)) {
                    player.actions.animate(SPADE_ANIM)
                    clearPatch(player, loc)
                } else {
                    player.ui.message("You need to have a spade in order to clear the dead plant.")
                }
            }
            // Bush/fruit-tree/tree only (checkHealthXp > 0) - secateurs-prune, must come before
            // the plain PLANT_CURE branch below so allotment/flower/herb keep hitting that one.
            slot.state == DISEASE && (cropFor(slot.itemId)?.checkHealthXp ?: 0) > 0 -> {
                if (player.inventory.contains(SECATEURS) || player.inventory.contains(MAGIC_SECATEURS)) {
                    player.actions.animate(if (loc.patchType == "bush") PRUNE_SECATEURS_ANIM else PRUNE_SECATEURS_ANIM - 1)
                    player.inventory.transaction { add(6020) }
                    if (player.random.chance(3, 4)) {
                        player.farmingState.writePatchSlot(loc.patchName, loc.slot, slot.itemId, GROWING, slot.compost, slot.stageOrLife, 0, slot.plantedBy)
                        player.farmingState.markDirty()
                        player.farmingState.notifyInteraction()
                        player.farmingState.refreshVisuals()
                    } else {
                        player.ui.message("You failed to cure the tree.")
                    }
                } else {
                    player.ui.message("You need to use a pair of secateurs to prune the tree.")
                }
            }
            slot.state == DISEASE -> {
                if (player.inventory.contains(PLANT_CURE)) {
                    player.actions.animate(CURING_ANIM)
                    player.inventory.transaction { remove(PLANT_CURE) }
                    player.farmingState.writePatchSlot(loc.patchName, loc.slot, slot.itemId, GROWING, slot.compost, slot.stageOrLife, 0, slot.plantedBy)
                    player.farmingState.markDirty()
                    player.farmingState.notifyInteraction()
                    player.farmingState.refreshVisuals()
                } else {
                    // Legacy shows this exact (wrong-item) message for the disease-cure refusal
                    // too - preserved verbatim, not "fixed", matching this effort's standing
                    // preserve-legacy-quirks-exactly policy.
                    player.ui.message("You need to use a spade in order to clear the dead plant.")
                }
            }
            // Bush/fruit-tree/tree only (checkHealthXp > 0) - no tool/space requirement, unlike
            // the plain allotment/flower/herb HARVEST branch below. Must come first.
            slot.state == HARVEST && (cropFor(slot.itemId)?.checkHealthXp ?: 0) > 0 -> {
                val crop = cropFor(slot.itemId)!!
                player.farmingState.writePatchSlot(loc.patchName, loc.slot, slot.itemId, PRODUCTION, slot.compost, productionLifeFor(loc.patchType), 0, slot.plantedBy)
                player.skills.gainXp(crop.checkHealthXp, Skill.FARMING)
                player.ui.message("You check the health of the ${crop.name.lowercase()} and it is in perfect condition.")
                player.farmingState.markDirty()
                player.farmingState.notifyInteraction()
                player.farmingState.refreshVisuals()
            }
            slot.state == HARVEST -> {
                if (player.inventory.contains(SPADE) && player.inventory.freeSlots() > 0) {
                    val crop = cropFor(slot.itemId)
                    if (crop != null) {
                        player.actions.animate(if (loc.patchType == "allotment") SPADE_ANIM else HARVEST_ANIM)
                        val life = slot.stageOrLife - 1
                        player.inventory.transaction { add(crop.harvestItem) }
                        player.skills.gainXp(crop.harvestXp, Skill.FARMING)
                        if (life <= 0) {
                            clearPatch(player, loc)
                        } else {
                            player.farmingState.writePatchSlot(loc.patchName, loc.slot, slot.itemId, HARVEST, slot.compost, life, slot.progress, slot.plantedBy)
                            player.farmingState.markDirty()
                            player.farmingState.notifyInteraction()
                            player.farmingState.refreshVisuals()
                        }
                    }
                } else if (player.inventory.freeSlots() <= 0) {
                    player.ui.message("You do not have enough inventory space!")
                } else {
                    player.ui.message("You need to use a spade in order to clear the dead plant.")
                }
            }
            // Bush/fruit-tree/tree only, entered from the checkHealthXp HARVEST branch above or
            // via the STUMP->PRODUCTION growth pulse.
            slot.state == PRODUCTION -> {
                val crop = cropFor(slot.itemId)
                when {
                    loc.patchType == "tree" -> {
                        player.farmingState.writePatchSlot(loc.patchName, loc.slot, slot.itemId, STUMP, slot.compost, slot.stageOrLife, slot.progress, slot.plantedBy)
                        player.farmingState.markDirty()
                        player.farmingState.notifyInteraction()
                        player.farmingState.refreshVisuals()
                    }
                    crop != null && slot.stageOrLife > 0 && player.inventory.freeSlots() > 0 -> {
                        player.actions.animate(if (loc.patchType.contains("tree")) HARVEST_FRUIT_ANIM else HARVEST_BUSH_ANIM)
                        player.inventory.transaction { add(crop.harvestItem) }
                        player.skills.gainXp(crop.harvestXp, Skill.FARMING)
                        player.farmingState.writePatchSlot(loc.patchName, loc.slot, slot.itemId, PRODUCTION, slot.compost, slot.stageOrLife - 1, slot.progress, slot.plantedBy)
                        player.farmingState.markDirty()
                        player.farmingState.notifyInteraction()
                        player.farmingState.refreshVisuals()
                    }
                    slot.stageOrLife == 0 && loc.patchType == "fruit tree" -> {
                        player.farmingState.writePatchSlot(loc.patchName, loc.slot, slot.itemId, STUMP, slot.compost, slot.stageOrLife, slot.progress, slot.plantedBy)
                        player.farmingState.markDirty()
                        player.farmingState.notifyInteraction()
                        player.farmingState.refreshVisuals()
                    }
                    slot.stageOrLife == 0 && loc.patchType == "bush" -> clearPatch(player, loc)
                    else -> player.ui.message("You do not have enough inventory space!")
                }
            }
            else -> return false
        }
        return true
    }

    /** The repeat-harvest life count once a checkHealthXp crop enters PRODUCTION - not persisted
     * (recomputed fresh from crop type every time), matching legacy's exact formula. */
    private fun productionLifeFor(patchType: String): Int = when (patchType) {
        "tree" -> 1
        "bush" -> 4
        else -> 6 // fruit tree
    }

    fun useItem(player: SkillPlayer, objectId: Int, itemId: Int): Boolean {
        val (loc, slot) = slotFor(player, objectId) ?: return false
        val itemName = player.inventory.itemName(itemId).lowercase()

        when {
            slot.state == WEED && slot.stageOrLife < 3 -> {
                if (itemId == RAKE) clickPatch(player, objectId) else player.ui.message("You need to use a rake in order to clear the weed.")
            }
            slot.state == DEAD -> {
                if (itemId == SPADE) clickPatch(player, objectId) else player.ui.message("You need to use a spade in order to clear the dead plant.")
            }
            slot.state == HARVEST -> {
                if (itemId == SPADE) clickPatch(player, objectId) else player.ui.message("You need to use a spade in order to harvest the patch.")
            }
            slot.state == DISEASE -> {
                // allotment/flower/herb never reach the tree/bush secateurs-prune branch.
                if (itemId == PLANT_CURE) clickPatch(player, objectId) else player.ui.message("You need to use a plant cure in order to cure the patch.")
            }
            (loc.patchType == "allotment" || loc.patchType == "flower") && itemId in wateringCanIds -> {
                if (slot.state == GROWING) {
                    player.actions.animate(WATERCAN_ANIM)
                    // Legacy resolves the "emptied one charge" can by name (charge "(1)" empties
                    // to the base 5331, everything else decrements by id) rather than a lookup
                    // table - preserved verbatim since the id gap at 5332 makes this arithmetic
                    // non-obvious to reconstruct any other way.
                    val emptiedCanId = if (itemName.endsWith("1)")) 5331 else itemId - 1
                    player.inventory.transaction { remove(itemId); add(emptiedCanId) }
                    player.farmingState.writePatchSlot(loc.patchName, loc.slot, slot.itemId, WATER, slot.compost, slot.stageOrLife, slot.progress, slot.plantedBy)
                    player.farmingState.markDirty()
                    player.farmingState.notifyInteraction()
                    player.farmingState.refreshVisuals()
                } else {
                    player.ui.message(
                        if (slot.state == PROTECTED) "The plant is already protected so no need to water the plant." else "The plant do not need watering.",
                    )
                }
            }
            itemId == COMPOST_ITEM_ID || itemId == SUPERCOMPOST_ITEM_ID || itemId == ULTRACOMPOST_ITEM_ID ->
                applyCompost(player, loc, slot, itemId, itemName)
            slot.itemId == -1 -> plantSeed(player, loc, slot, itemId)
            else -> clickPatch(player, objectId)
        }
        return true
    }

    private fun applyCompost(player: SkillPlayer, loc: PatchLocation, slot: SkillPatchSlot, itemId: Int, itemName: String) {
        val currentTier = compostTierOrdinal(slot.compost)
        val tiers = listOf(Triple("COMPOST", COMPOST_ITEM_ID, 1), Triple("SUPERCOMPOST", SUPERCOMPOST_ITEM_ID, 2), Triple("ULTRACOMPOST", ULTRACOMPOST_ITEM_ID, 3))
        for ((name, compostItemId, ordinal) in tiers) {
            if (itemId != compostItemId) continue
            if (currentTier < ordinal) {
                player.actions.animate(COMPOST_PATCH_ANIM)
                player.inventory.transaction { remove(itemId); add(BUCKET) }
                player.farmingState.writePatchSlot(loc.patchName, loc.slot, slot.itemId, slot.state, name, slot.stageOrLife, slot.progress, slot.plantedBy)
                player.farmingState.markDirty()
                player.farmingState.notifyInteraction()
            } else {
                // Legacy's exact (bug-preserving) concatenation: "supercompost"/"ultracompost"
                // already end in "compost", so this doubles the word for those two tiers -
                // preserved verbatim, not fixed.
                player.ui.message(
                    "There is no point in using $itemName when the patch already got ${slot.compost.lowercase()}" +
                        if (slot.compost != "COMPOST") "compost." else ".",
                )
            }
            return
        }
    }

    private fun compostTierOrdinal(compost: String): Int = when (compost) {
        "COMPOST" -> 1
        "SUPERCOMPOST" -> 2
        "ULTRACOMPOST" -> 3
        else -> 0
    }

    private fun plantSeed(player: SkillPlayer, loc: PatchLocation, slot: SkillPatchSlot, itemId: Int) {
        val crop = cropFor(itemId) ?: return
        if (crop.type != loc.patchType) {
            player.ui.message("${player.inventory.itemName(itemId)} can only be planted at a ${crop.type} patch.")
            return
        }
        val tool = if (crop.type.contains("tree")) SPADE else SEED_DIBBER
        if (!player.inventory.contains(tool)) {
            player.ui.message("You are missing the ${player.inventory.itemName(tool).lowercase()} tool.")
            return
        }
        if (player.skills.current(Skill.FARMING) < crop.level) {
            player.ui.message("You need level ${crop.level} farming to plant ${player.inventory.itemName(itemId).lowercase()}.")
            return
        }
        val amount = if (crop.type == "allotment") 3 else 1
        if (!player.inventory.contains(itemId, amount)) {
            player.ui.message("You need $amount ${player.inventory.itemName(itemId).lowercase()} to plant here.")
            return
        }
        player.actions.animate(if (crop.type.contains("tree")) SPADE_ANIM else PLANTSEED_ANIM)
        player.inventory.transaction {
            remove(itemId, amount)
            // Tree/fruit-tree saplings are grown in a plant pot (SaplingItemCombinations.kt,
            // legacy-owned, out of scope) - planting one onto the patch empties the pot.
            if (crop.type.contains("tree")) add(EMPTY_PLANT_POT)
        }
        player.skills.gainXp(crop.plantXp, Skill.FARMING)
        player.farmingState.writePatchSlot(loc.patchName, loc.slot, itemId, GROWING, slot.compost, 1, 0, System.currentTimeMillis().toInt())
        player.farmingState.markDirty()
        player.farmingState.notifyInteraction()
        player.farmingState.refreshVisuals()
    }

    fun inspectPatch(player: SkillPlayer, objectId: Int) {
        val (loc, slot) = slotFor(player, objectId) ?: return
        val crop = cropFor(slot.itemId)
        val weeding = slot.state == WEED
        val growing = slot.state == GROWING
        val disease = slot.state == DISEASE
        val dead = slot.state == DEAD

        val messageOne = "This is a ${loc.patchType} patch."
        val messageTwo = when {
            slot.compost == NO_COMPOST -> "The soil has not been treated."
            slot.compost == PROTECTED -> "This patch is protected."
            else -> "The soil has been treated with ${slot.compost.lowercase()}."
        }
        val messageThree = when {
            slot.stageOrLife < 3 && weeding -> "The patch needs weeding."
            slot.itemId == -1 -> "The patch is empty."
            dead -> "Patch is dead!"
            disease -> "Currently diseased, you should try and cure it."
            growing -> "The patch has ${crop?.name?.lowercase() ?: ""} growing in it and is at state ${slot.stageOrLife}/${crop?.stages ?: 0}"
            else -> "The patch (${crop?.name?.lowercase() ?: ""}) is fully grown."
        }
        player.ui.message("$messageOne $messageTwo $messageThree")
    }

    // --- Compost bins: fill/open/extract/dump cycle ---------------------------------------------
    // Reproduces legacy Farming.kt's Client.interactBin/Client.interactItemBin/Client.examineBin's
    // compost-bin logic exactly. examineBin is reached from PlayerCore.examineObject (a real,
    // live op10 dispatch path via ExamineListener.java/ObjectExamineBootstrap.kt) - it is called
    // directly from there (Java can't see the SkillInteractionDispatcher route the other bindings
    // use), matching the same "direct call into the plugin object" pattern already used by
    // Mazchna.kt/Vannaka.kt/Duradel.kt's redirects.

    private val binLocations: Map<Int, String> = mapOf(
        7837 to "CATHERBY",
        7839 to "ARDOUGNE",
        7838 to "CANAFIS",
        1003 to "FALADOR",
    )

    private const val BIN_EMPTY = "EMPTY"
    private const val BIN_FILLED = "FILLED"
    private const val BIN_CLOSED = "CLOSED"
    private const val BIN_DONE = "DONE"
    private const val BIN_OPEN = "OPEN"

    private fun binFor(player: SkillPlayer, objectId: Int): Pair<String, SkillCompostBinState>? {
        val binName = binLocations[objectId] ?: return null
        val bin = player.farmingState.compostBins().firstOrNull { it.binName == binName }
            ?: SkillCompostBinState(binName, NO_COMPOST, BIN_EMPTY, 0, 0)
        return binName to bin
    }

    private fun compostItemFor(compostName: String): Int = when (compostName) {
        "COMPOST" -> COMPOST_ITEM_ID
        "SUPERCOMPOST" -> SUPERCOMPOST_ITEM_ID
        "ULTRACOMPOST" -> ULTRACOMPOST_ITEM_ID
        else -> COMPOST_ITEM_ID
    }

    fun examineBin(player: SkillPlayer, objectId: Int) {
        val (_, bin) = binFor(player, objectId) ?: return
        player.ui.message(
            when (bin.state) {
                BIN_CLOSED -> "The bin is currently in the process of rotting the containment."
                BIN_DONE -> "The ${bin.compost.lowercase()} is ready."
                BIN_EMPTY -> "The bin is currently empty."
                BIN_OPEN -> "There is currently ${bin.amount}/15 ${bin.compost.lowercase()} remaining."
                else -> "There is currently ${bin.amount}/15 ${bin.compost.lowercase()} filled."
            },
        )
    }

    fun interactBin(player: SkillPlayer, objectId: Int, option: Int) {
        val (binName, bin) = binFor(player, objectId) ?: return
        when (option) {
            1 -> when {
                bin.state == BIN_FILLED && bin.amount == 15 -> {
                    player.farmingState.writeCompostBin(binName, bin.compost, BIN_CLOSED, bin.amount, bin.progress)
                    player.farmingState.markDirty()
                    player.farmingState.notifyInteraction()
                    player.farmingState.refreshVisuals()
                }
                bin.state == BIN_DONE -> {
                    player.farmingState.writeCompostBin(binName, bin.compost, BIN_OPEN, bin.amount, bin.progress)
                    player.farmingState.markDirty()
                    player.farmingState.notifyInteraction()
                    player.farmingState.refreshVisuals()
                }
                bin.state == BIN_OPEN -> {
                    if (player.inventory.contains(BUCKET)) {
                        player.inventory.transaction { remove(BUCKET); add(compostItemFor(bin.compost)) }
                        val amount = bin.amount - 1
                        if (amount == 0) {
                            player.farmingState.writeCompostBin(binName, NO_COMPOST, BIN_EMPTY, 0, 0)
                        } else {
                            player.farmingState.writeCompostBin(binName, bin.compost, BIN_OPEN, amount, bin.progress)
                        }
                        player.farmingState.markDirty()
                        player.farmingState.notifyInteraction()
                        player.farmingState.refreshVisuals()
                    } else {
                        player.ui.message("You are missing a bucket to be filled with compost.")
                    }
                }
            }
            5 -> player.ui.dialogue {
                options(
                    title = "You wish to empty the compost bin?",
                    SkillDialogueOption("Yes") {
                        finishThen { p ->
                            // Legacy quirk, preserved verbatim: the dump handler resets compost
                            // type/state/progress to NONE/EMPTY/0 but never explicitly clears
                            // `amount` (it's only zeroed elsewhere as a side effect of the
                            // extract-to-0 path) - so amount is left stale after a dump. Not
                            // fixed, matching this effort's standing preserve-over-fix default.
                            p.farmingState.writeCompostBin(binName, NO_COMPOST, BIN_EMPTY, bin.amount, 0)
                            p.farmingState.markDirty()
                            p.farmingState.notifyInteraction()
                            p.farmingState.refreshVisuals()
                            p.ui.message("You dump all the content inside the bin!")
                        }
                    },
                    SkillDialogueOption("No") { finish() },
                )
            }
        }
    }

    private fun fillBinAs(player: SkillPlayer, binName: String, bin: SkillCompostBinState, itemId: Int, compostType: String) {
        player.inventory.transaction { remove(itemId) }
        player.farmingState.writeCompostBin(binName, compostType, BIN_FILLED, bin.amount + 1, bin.progress)
        player.farmingState.markDirty()
        player.farmingState.notifyInteraction()
        player.farmingState.refreshVisuals()
    }

    fun useItemOnBin(player: SkillPlayer, objectId: Int, itemId: Int): Boolean {
        val (binName, bin) = binFor(player, objectId) ?: return false
        val itemName = player.inventory.itemName(itemId).lowercase()

        if (itemId == BUCKET && bin.amount > 0 && bin.state == BIN_OPEN) {
            interactBin(player, objectId, 1)
            return true
        }
        if (itemId == VOLCANIC_ASH && bin.amount == 15 && bin.state == BIN_OPEN) {
            if (player.inventory.contains(VOLCANIC_ASH, 25)) {
                player.inventory.transaction { remove(VOLCANIC_ASH, 25) }
                player.farmingState.writeCompostBin(binName, "ULTRACOMPOST", bin.state, bin.amount, bin.progress)
                player.farmingState.markDirty()
                player.farmingState.notifyInteraction()
            } else {
                player.ui.message("You need 25 $itemName in order to convert into ultra compost.")
            }
            return true
        }

        // Not empty, and not still-fillable (FILLED with room left) - refuse with the matching
        // legacy message for whichever state it's actually in.
        if (bin.state != BIN_EMPTY && !(bin.state == BIN_FILLED && bin.amount < 15)) {
            player.ui.message(
                when {
                    bin.state == BIN_CLOSED -> "The bin is currently in the process of rotting the containment."
                    bin.state == BIN_FILLED && bin.amount == 15 -> "The bin is currently full!"
                    bin.state == BIN_OPEN && bin.amount > 0 -> "Empty the bin before you try and fill it!"
                    else -> "The bin is done rotting the containment; Perhaps you should open it?"
                },
            )
            return false
        }

        val isSuper = itemId in superCompostItemIds
        val isRegular = itemId in regularCompostItemIds
        if (!isSuper && !isRegular) {
            player.ui.message("This item has no use to be put into the bin.")
            return false
        }

        // Both cross-tier confirmations intentionally force the bin to plain COMPOST (not the
        // item's own tier) - legacy hardcodes this in both dialogs (they're both "use this item
        // as if it were regular compost" flows), not a bug - preserved as the direct-insert
        // path below (which does pick the item's own tier) is the only place that varies.
        if (bin.state != BIN_EMPTY && isSuper && bin.compost != "SUPERCOMPOST") {
            player.ui.dialogue {
                options(
                    title = "You wish to convert to use this item for regular compost",
                    SkillDialogueOption("Yes") { finishThen { p -> fillBinAs(p, binName, bin, itemId, "COMPOST") } },
                    SkillDialogueOption("No") { finish() },
                )
            }
            return true
        }
        if (isRegular && bin.compost == "SUPERCOMPOST") {
            player.ui.dialogue {
                options(
                    title = "You wish to convert to regular compost?",
                    SkillDialogueOption("Yes") { finishThen { p -> fillBinAs(p, binName, bin, itemId, "COMPOST") } },
                    SkillDialogueOption("No") { finish() },
                )
            }
            return true
        }

        fillBinAs(player, binName, bin, itemId, if (isRegular) "COMPOST" else "SUPERCOMPOST")
        return true
    }

    // --- Growth-tick simulation: one wall-clock pulse's worth of patch/bin progression ---------
    // Called by the engine's FarmingRuntimeService catch-up scheduler (once per due pulse, not
    // once per game tick). Reproduces legacy Farming.kt's updateFarming() exactly for allotment/
    // flower/herb patches and compost bins - STUMP/PRODUCTION (bush/fruit-tree/tree) are
    // confirmed dead (no live object ids exist anywhere) and were not ported; the sapling bank/
    // inventory auto-conversion loop stays legacy-owned (Farming.kt's updateSaplings(), out of
    // scope - bush/fruit-tree/tree territory).

    private val binTicks: Map<String, Int> = mapOf(
        "CATHERBY" to 1,
        "ARDOUGNE" to 1,
        "CANAFIS" to 1,
        "FALADOR" to 60,
    )

    private fun compostDivideValue(compost: String): Int = when (compost) {
        "COMPOST" -> 4
        "SUPERCOMPOST" -> 8
        "ULTRACOMPOST" -> 12
        else -> 1
    }

    fun applyGrowthPulse(player: SkillPlayer) {
        val patchSlotsByKey = player.farmingState.patchSlots().associateBy { it.patchName to it.slot }
        patchLocations.values.forEach { loc ->
            val slot = patchSlotsByKey[loc.patchName to loc.slot]
                ?: SkillPatchSlot(loc.patchName, loc.slot, -1, WEED, NO_COMPOST, 0, 0, -1)
            applyPatchPulse(player, loc, slot)
        }
        player.farmingState.compostBins().forEach { bin -> applyBinPulse(player, bin) }
    }

    private fun applyPatchPulse(player: SkillPlayer, loc: PatchLocation, slot: SkillPatchSlot) {
        val crop = cropFor(slot.itemId)
        when {
            slot.state == WEED && slot.stageOrLife > 0 -> {
                val progress = slot.progress + 1
                if (progress == 3) {
                    player.farmingState.writePatchSlot(loc.patchName, loc.slot, slot.itemId, WEED, slot.compost, slot.stageOrLife - 1, 0, slot.plantedBy)
                    player.farmingState.markDirty()
                    player.farmingState.refreshVisuals()
                    player.farmingState.notifyInteraction()
                } else {
                    player.farmingState.writePatchSlot(loc.patchName, loc.slot, slot.itemId, WEED, slot.compost, slot.stageOrLife, progress, slot.plantedBy)
                }
            }
            slot.itemId != -1 && crop != null && (slot.state == GROWING || slot.state == WATER || slot.state == PROTECTED) && slot.stageOrLife < crop.stages -> {
                val progress = slot.progress + 1
                if (progress >= crop.growTick) {
                    var newState = if (slot.state == WATER) GROWING else slot.state
                    var stageOrLife = slot.stageOrLife + 1
                    var diseaseChance = if (slot.state == WATER) crop.diseaseChance * 0.9 else crop.diseaseChance.toDouble()
                    diseaseChance *= (1.0 - ((player.skills.current(Skill.FARMING).toDouble() / 2.85) / 100))
                    diseaseChance /= compostDivideValue(slot.compost)
                    if (stageOrLife == crop.stages) {
                        newState = HARVEST
                        stageOrLife = if (crop.type == "allotment" || crop.type == "herb") 3 + compostTierOrdinal(slot.compost) else 1
                    } else if (slot.state != PROTECTED && player.random.chance(diseaseChance.roundToInt().coerceIn(0, 1024), 1024)) {
                        newState = DISEASE
                    }
                    player.farmingState.writePatchSlot(loc.patchName, loc.slot, slot.itemId, newState, slot.compost, stageOrLife, 0, slot.plantedBy)
                    player.farmingState.markDirty()
                    player.farmingState.refreshVisuals()
                    player.farmingState.notifyInteraction()
                } else {
                    player.farmingState.writePatchSlot(loc.patchName, loc.slot, slot.itemId, slot.state, slot.compost, slot.stageOrLife, progress, slot.plantedBy)
                }
            }
            slot.itemId != -1 && crop != null && slot.state == DISEASE -> {
                val progress = slot.progress + 1
                if (progress == crop.growTick * 2) {
                    player.farmingState.writePatchSlot(loc.patchName, loc.slot, slot.itemId, DEAD, slot.compost, slot.stageOrLife, 0, slot.plantedBy)
                    player.farmingState.markDirty()
                    player.farmingState.refreshVisuals()
                    player.farmingState.notifyInteraction()
                } else {
                    player.farmingState.writePatchSlot(loc.patchName, loc.slot, slot.itemId, DISEASE, slot.compost, slot.stageOrLife, progress, slot.plantedBy)
                }
            }
            // Bush/fruit-tree/tree only, regrowing after a click chopped/exhausted it to STUMP -
            // doesn't touch stageOrLife (legacy doesn't either - the PRODUCTION-regrowth branch
            // below always recomputes the life cap fresh from crop type, not from stored state).
            slot.itemId != -1 && crop != null && slot.state == STUMP -> {
                val progress = slot.progress + 1
                if (progress >= crop.growTick / 3) {
                    player.farmingState.writePatchSlot(loc.patchName, loc.slot, slot.itemId, PRODUCTION, slot.compost, slot.stageOrLife, 0, slot.plantedBy)
                    player.farmingState.markDirty()
                    player.farmingState.refreshVisuals()
                    player.farmingState.notifyInteraction()
                } else {
                    player.farmingState.writePatchSlot(loc.patchName, loc.slot, slot.itemId, STUMP, slot.compost, slot.stageOrLife, progress, slot.plantedBy)
                }
            }
            // Regrows stageOrLife back up to the fresh per-type life cap after a harvest
            // decremented it (or after a STUMP->PRODUCTION reset left it wherever it was).
            slot.itemId != -1 && crop != null && slot.state == PRODUCTION && slot.stageOrLife < productionLifeFor(loc.patchType) -> {
                val progress = slot.progress + 1
                if (progress >= crop.growTick / 3) {
                    player.farmingState.writePatchSlot(loc.patchName, loc.slot, slot.itemId, PRODUCTION, slot.compost, slot.stageOrLife + 1, 0, slot.plantedBy)
                    player.farmingState.markDirty()
                    player.farmingState.refreshVisuals()
                    player.farmingState.notifyInteraction()
                } else {
                    player.farmingState.writePatchSlot(loc.patchName, loc.slot, slot.itemId, PRODUCTION, slot.compost, slot.stageOrLife, progress, slot.plantedBy)
                }
            }
        }
    }

    private fun applyBinPulse(player: SkillPlayer, bin: SkillCompostBinState) {
        if (bin.state != BIN_CLOSED) return
        val ticks = binTicks[bin.binName] ?: return
        val progress = bin.progress + 1
        if (progress >= ticks) {
            player.farmingState.writeCompostBin(bin.binName, bin.compost, BIN_DONE, bin.amount, 0)
            player.farmingState.markDirty()
            player.farmingState.refreshVisuals()
            player.farmingState.notifyInteraction()
        } else {
            player.farmingState.writeCompostBin(bin.binName, bin.compost, bin.state, bin.amount, progress)
        }
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
                stages = row.getValue("stages").toInt(),
                growTick = row.getValue("grow_tick").toInt(),
                diseaseChance = row.getValue("disease_chance").toInt(),
                checkHealthXp = row.getValue("check_health_xp").toInt(),
            )
        }
}
