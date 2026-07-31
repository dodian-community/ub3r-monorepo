package net.dodian.uber.skills.crafting

import net.dodian.uber.game.api.content.ContentAttributeKey
import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.skills.SkillButtonInteraction
import net.dodian.uber.game.api.plugin.skills.SkillItemOnItemInteraction
import net.dodian.uber.game.api.plugin.skills.SkillObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillPlayer
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.SkillPluginDefinition
import net.dodian.uber.game.api.plugin.skills.manifest
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.api.plugin.skills.startProduction
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.skill.runtime.action.productionAction
import net.dodian.uber.skills.api.SkillModuleDescriptor
import net.dodian.uber.skills.api.SkillMultiAction
import net.dodian.uber.skills.api.SkillMultiConfig
import net.dodian.uber.skills.api.SkillMultiEntry
import net.dodian.uber.skills.api.skillRecipe
import net.dodian.uber.game.api.plugin.runtime.TomlRecordReader

data class TanningDef(val hideType: Int, val hideId: Int, val leatherId: Int, val coinCost: Int)

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

    // Legacy spinning wheel objects (skill.crafting.Crafting.performSpin/spinDelayMs, orphaned by
    // the old content.objects.impl.crafting.SpinningWheelObjects registration that used to wire
    // these up). Flax takes priority over wool when both are carried, matching that behavior.
    // 4309 is the real, live-world Seers Village spinning wheel (definitions/objects/spawns/
    // seers_village/spinning_wheel_flax.toml) - it was unregistered anywhere, legacy or plugin,
    // until this fix; 14889/25824 are valid spinning-wheel definitions with no confirmed
    // real-world placement, kept for compatibility with whatever wired them up historically.
    private val spinningWheelObjectIds = intArrayOf(14889, 25824, 4309)
    private const val FLAX_ITEM_ID = 1779
    private const val BOW_STRING_ITEM_ID = 1777
    private const val FLAX_SPIN_EXPERIENCE = 50
    private const val WOOL_ITEM_ID = 1737
    private const val BALL_OF_WOOL_ITEM_ID = 1759
    private const val WOOL_SPIN_EXPERIENCE = 100

    // Tanner npc (Tanner.kt) - talk-to (option 1) opens the tanning interface. hideType 1 has
    // no definition below, matching the legacy TanningDefinitions gap: its button silently no-ops.
    private const val TANNER_NPC_ID = 5809
    private const val TANNING_INTERFACE_ID = 14670
    private val tanningDefs = listOf(
        TanningDef(0, 1739, 1741, 50),
        TanningDef(2, 1753, 1745, 1000),
        TanningDef(3, 1751, 2505, 2000),
        TanningDef(4, 1749, 2507, 5000),
        TanningDef(5, 1747, 2509, 10000),
    )
    private val tanningButtonGroups = listOf(
        0 to intArrayOf(57225, 57217, 57201, 57209),
        1 to intArrayOf(57229, 57221, 57205, 57213),
        2 to intArrayOf(57227, 57219, 57211, 57203),
        3 to intArrayOf(57228, 57220, 57212, 57204),
        4 to intArrayOf(57231, 57223, 57215, 57207),
        5 to intArrayOf(57232, 57224, 57216, 57208),
    )
    private val tanningAmountByButton = mapOf(
        57225 to 1, 57217 to 5, 57201 to 27, 57209 to 27,
        57229 to 1, 57221 to 5, 57205 to 27, 57213 to 27,
        57227 to 1, 57219 to 5, 57211 to 27, 57203 to 27,
        57228 to 1, 57220 to 5, 57212 to 27, 57204 to 27,
        57231 to 1, 57223 to 5, 57215 to 27, 57207 to 27,
        57232 to 1, 57224 to 5, 57216 to 27, 57208 to 27,
    )

    // Molten glass (1775) + bucket for water (1785) opens the vial/cup/fishbowl/orb picker.
    private const val MOLTEN_GLASS_ITEM_ID = 1775
    private const val BUCKET_ITEM_ID = 1785
    private const val GLASS_INTERFACE_ID = 11462

    data class GlassProduct(val buttonIds: IntArray, val productId: Int, val minimumLevel: Int, val levelMessage: String?, val experiencePerUnit: Int)
    private val glassProducts = listOf(
        GlassProduct(intArrayOf(44210, 44209, 44208, 44207), 229, 1, null, 80),
        GlassProduct(intArrayOf(48108, 48107, 48106, 48105), 1980, 18, "You need level 18 crafting to craft a empty cup.", 120),
        GlassProduct(intArrayOf(48112, 48111, 48110, 48109), 6667, 32, "You need level 32 crafting to craft a fishbowl.", 160),
        GlassProduct(intArrayOf(48116, 48115, 48114, 48113), 567, 48, "You need level 48 crafting to craft a unpowered orb.", 240),
    )
    private val glassAmountByButton = mapOf(
        44210 to 27, 44209 to 10, 44208 to 5, 44207 to 1,
        48108 to 27, 48107 to 10, 48106 to 5, 48105 to 1,
        48112 to 27, 48111 to 10, 48110 to 5, 48109 to 1,
        48116 to 27, 48115 to 10, 48114 to 5, 48113 to 1,
    )

    private const val CRYSTAL_KEY_HALF_A = 2382
    private const val CRYSTAL_KEY_HALF_B = 2383
    private const val CRYSTAL_KEY_ID = 989

    private const val FISHBOWL_ID = 6667
    private const val FISHBOWL_HELMET_ID = 7534

    // Wool + unstrung amulet -> strung amulet, ported from the legacy GoldJewelryDefinitions'
    // amulet group (jewelryByGroup[2] / strungAmulets) - the only still-live piece of that
    // system; the mould-crafting interface itself is confirmed dead code (see plan).
    private val unstrungToStrungAmuletIds = mapOf(
        1673 to 1692, 1675 to 1694, 1677 to 1696, 1679 to 1698, 1681 to 1700, 1683 to 1702, 6579 to 6581,
    )

    override val definition: SkillPluginDefinition = skillPlugin("Crafting", Skill.CRAFTING) {
        gems.forEach { gem ->
            itemOnItem(PolicyPreset.PRODUCTION, CHISEL_ITEM_ID, gem.uncutId) { cutGem(it, gem) }
        }
        hides.forEach { hide ->
            itemOnItem(PolicyPreset.PRODUCTION, NEEDLE_ITEM_ID, hide.itemId) { craftHide(it, hide) }
        }
        objectClick(preset = PolicyPreset.PRODUCTION, option = 2, *spinningWheelObjectIds) { interaction ->
            startSpinning(interaction)
        }
        npcClick(PolicyPreset.PRODUCTION, option = 1, TANNER_NPC_ID) { interaction ->
            openTanning(interaction.player)
            true
        }
        tanningButtonGroups.forEach { (hideType, buttonIds) ->
            button(PolicyPreset.PRODUCTION, requiredInterfaceId = TANNING_INTERFACE_ID, rawButtonIds = buttonIds) { interaction ->
                craftTanning(interaction, hideType)
            }
        }
        itemOnItem(PolicyPreset.PRODUCTION, MOLTEN_GLASS_ITEM_ID, BUCKET_ITEM_ID) { interaction ->
            openGlassPicker(interaction.player)
            true
        }
        glassProducts.forEach { product ->
            button(PolicyPreset.PRODUCTION, requiredInterfaceId = GLASS_INTERFACE_ID, rawButtonIds = product.buttonIds) { interaction ->
                craftGlassProduct(interaction, product)
            }
        }
        itemOnItem(PolicyPreset.PRODUCTION, CRYSTAL_KEY_HALF_A, CRYSTAL_KEY_HALF_B) { interaction ->
            craftCrystalKey(interaction)
        }
        itemOnItem(PolicyPreset.PRODUCTION, FISHBOWL_ID, CHISEL_ITEM_ID) { interaction ->
            craftFishbowlHelmet(interaction)
        }
        unstrungToStrungAmuletIds.keys.forEach { amuletId ->
            itemOnItem(PolicyPreset.PRODUCTION, BALL_OF_WOOL_ITEM_ID, amuletId) { interaction ->
                stringAmulet(interaction)
            }
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

    private const val THREAD_ITEM_ID = 1734
    private const val SEWING_ANIMATION_ID = 1249

    private fun craftHide(interaction: SkillItemOnItemInteraction, hide: CraftingHideDef): Boolean {
        val player = interaction.player
        if (player.skills.current(Skill.CRAFTING) < hide.glovesLevel) {
            player.ui.message("You need level ${hide.glovesLevel} crafting to craft from this leather.")
            return true
        }
        if (player.inventory.amount(THREAD_ITEM_ID) <= 0) {
            player.ui.message("You need thread to craft with leather.")
            return true
        }

        val glovesRecipe = skillRecipe("crafting.hide.${hide.glovesId}", hide.glovesId) {
            material(hide.itemId, 1)
            material(THREAD_ITEM_ID, 1)
            requirement(hide.glovesLevel)
            experience(hide.experience)
            animation(SEWING_ANIMATION_ID)
            delay(3)
            success("You make a pair of gloves.")
        }
        val chapsRecipe = skillRecipe("crafting.hide.${hide.chapsId}", hide.chapsId) {
            material(hide.itemId, 2)
            material(THREAD_ITEM_ID, 1)
            requirement(hide.chapsLevel)
            experience(hide.experience * 2)
            animation(SEWING_ANIMATION_ID)
            delay(3)
            success("You make a pair of chaps.")
        }
        val bodyRecipe = skillRecipe("crafting.hide.${hide.bodyId}", hide.bodyId) {
            material(hide.itemId, 3)
            material(THREAD_ITEM_ID, 1)
            requirement(hide.bodyLevel)
            experience(hide.experience * 3)
            animation(SEWING_ANIMATION_ID)
            delay(3)
            success("You make a body armor.")
        }

        val entries = mutableListOf(SkillMultiEntry(glovesRecipe))
        if (player.skills.current(Skill.CRAFTING) >= hide.chapsLevel) entries.add(SkillMultiEntry(chapsRecipe))
        if (player.skills.current(Skill.CRAFTING) >= hide.bodyLevel) entries.add(SkillMultiEntry(bodyRecipe))

        return player.production.open(
            SkillMultiConfig(
                key = "crafting.hide.${hide.itemId}",
                verb = "make",
                action = SkillMultiAction.MAKE,
                entries = entries,
            )
        ) { selection ->
            val selectedRecipe = when (selection.recipeKey) {
                chapsRecipe.key -> chapsRecipe
                bodyRecipe.key -> bodyRecipe
                else -> glovesRecipe
            }
            player.startProduction(selectedRecipe, selection.amount, Skill.CRAFTING)
        }
    }

    private fun startSpinning(interaction: SkillObjectInteraction): Boolean {
        val player = interaction.player
        productionAction("spinning") {
            delay { spinDelayTicks(player) }
            onCycleSignal { performSpin(player) }
        }.start(player)
        return true
    }

    private fun spinDelayTicks(player: SkillPlayer): Int =
        when {
            player.skills.current(Skill.CRAFTING) >= 70 -> 1
            player.skills.current(Skill.CRAFTING) >= 40 -> 2
            else -> 3
        }

    // Mirrors legacy Crafting.performSpin exactly: flax takes priority over wool, and there is
    // no crafting-level gate on either (a quirk of the original implementation, kept as-is).
    private fun performSpin(player: SkillPlayer): net.dodian.uber.game.skill.runtime.action.CycleSignal {
        if (player.inventory.transaction { remove(FLAX_ITEM_ID, 1); add(BOW_STRING_ITEM_ID, 1) }) {
            player.skills.gainXp(FLAX_SPIN_EXPERIENCE, Skill.CRAFTING)
            player.actions.triggerRandomEvent(FLAX_SPIN_EXPERIENCE)
        } else if (player.inventory.transaction { remove(WOOL_ITEM_ID, 1); add(BALL_OF_WOOL_ITEM_ID, 1) }) {
            player.skills.gainXp(WOOL_SPIN_EXPERIENCE, Skill.CRAFTING)
            player.actions.triggerRandomEvent(WOOL_SPIN_EXPERIENCE)
        } else {
            player.ui.message("You do not have anything to spin!")
            return net.dodian.uber.game.skill.runtime.action.CycleSignal.stop()
        }
        return net.dodian.uber.game.skill.runtime.action.CycleSignal.success()
    }

    // Reproduces legacy Tanning.open's exact string/model pokes verbatim.
    private fun openTanning(player: SkillPlayer) {
        player.ui.string("Regular Leather", 14777)
        player.ui.string("50gp", 14785)
        player.ui.string("", 14781)
        player.ui.string("", 14789)
        player.ui.string("", 14778)
        player.ui.string("", 14786)
        player.ui.string("", 14782)
        player.ui.string("", 14790)
        player.ui.string("Green", 14779)
        player.ui.string("1,000gp", 14787)
        player.ui.string("Blue", 14783)
        player.ui.string("2,000gp", 14791)
        player.ui.string("Red", 14780)
        player.ui.string("5,000gp", 14788)
        player.ui.string("Black", 14784)
        player.ui.string("10,000gp", 14792)
        player.ui.itemModel(14769, 250, 1741)
        player.ui.itemModel(14773, 250, -1)
        player.ui.itemModel(14771, 250, 1753)
        player.ui.itemModel(14772, 250, 1751)
        player.ui.itemModel(14775, 250, 1749)
        player.ui.itemModel(14776, 250, 1747)
        player.ui.open(TANNING_INTERFACE_ID)
    }

    // Reproduces legacy Tanning.start exactly, including the missing hideType=1 definition
    // (that button silently does nothing - a pre-existing gap in TanningDefinitions, preserved).
    private fun craftTanning(interaction: SkillButtonInteraction, hideType: Int): Boolean {
        val player = interaction.player
        val definition = tanningDefs.firstOrNull { it.hideType == hideType } ?: return true
        val requestedAmount = tanningAmountByButton[interaction.rawButtonId] ?: return false
        if (!player.inventory.contains(995, definition.coinCost)) {
            player.ui.message("You need atleast ${definition.coinCost} coins to do this!")
            return true
        }
        var amount = requestedAmount
        val coins = player.inventory.amount(995)
        amount = if (coins > amount * definition.coinCost) coins / definition.coinCost else amount
        amount = minOf(amount, player.inventory.amount(definition.hideId))
        repeat(amount.coerceAtLeast(0)) {
            player.inventory.transaction {
                remove(definition.hideId, 1)
                remove(995, definition.coinCost)
                add(definition.leatherId, 1)
            }
        }
        return true
    }

    // Reproduces legacy CraftingItemCombinations.handle's molten-glass branch verbatim.
    private fun openGlassPicker(player: SkillPlayer) {
        val jump = "\n\n\n"
        player.ui.itemModel(11465, 160, 229)
        player.ui.string(jump + "Vial", 11474)
        player.ui.itemModel(11466, 180, 1980)
        player.ui.string(jump + "Empty cup", 12396)
        player.ui.itemModel(11467, 150, 6667)
        player.ui.string(jump + "Fishbowl", 12400)
        player.ui.itemModel(11468, 150, 567)
        player.ui.string(jump + "Orb", 12404)
        player.ui.itemModel(11469, 190, -1)
        player.ui.string(jump, 12408)
        player.ui.itemModel(11470, 190, -1)
        player.ui.string(jump, 12412)
        player.ui.itemModel(6199, 190, -1)
        player.ui.string(jump, 6203)
        player.ui.open(GLASS_INTERFACE_ID)
    }

    private val glassRemainingKey = ContentAttributeKey<Int>("skill.crafting", "glassRemaining")

    private fun craftGlassProduct(interaction: SkillButtonInteraction, product: GlassProduct): Boolean {
        val player = interaction.player
        player.ui.close()
        if (product.minimumLevel > 1 && player.skills.current(Skill.CRAFTING) < product.minimumLevel) {
            product.levelMessage?.let { player.ui.message(it) }
            return true
        }
        val amount = glassAmountByButton[interaction.rawButtonId] ?: return false
        player.attributes.put(glassRemainingKey, amount.coerceAtLeast(1))
        productionAction("crafting.glass.${product.productId}") {
            delay(3)
            onCycleSignal {
                val remaining = attributes.get(glassRemainingKey) ?: 0
                if (remaining <= 0) return@onCycleSignal net.dodian.uber.game.skill.runtime.action.CycleSignal.stop()
                if (!inventory.transaction { remove(MOLTEN_GLASS_ITEM_ID, 1); add(product.productId, 1) }) {
                    return@onCycleSignal net.dodian.uber.game.skill.runtime.action.CycleSignal.stop()
                }
                skills.gainXp(product.experiencePerUnit, Skill.CRAFTING)
                val nextRemaining = remaining - 1
                attributes.put(glassRemainingKey, nextRemaining)
                if (nextRemaining <= 0) net.dodian.uber.game.skill.runtime.action.CycleSignal.completeSuccess()
                else net.dodian.uber.game.skill.runtime.action.CycleSignal.success()
            }
            onStop { attributes.remove(glassRemainingKey) }
        }.start(player)
        return true
    }

    private fun craftCrystalKey(interaction: SkillItemOnItemInteraction): Boolean {
        val player = interaction.player
        if (player.skills.current(Skill.CRAFTING) < 60) {
            player.ui.message("You need 60 crafting to make the crystal key")
            return true
        }
        if (player.inventory.transaction { remove(CRYSTAL_KEY_HALF_A, 1); remove(CRYSTAL_KEY_HALF_B, 1); add(CRYSTAL_KEY_ID, 1) }) {
            player.ui.message("You have crafted the crystal key!  I wonder what it's for?")
        }
        return true
    }

    private fun craftFishbowlHelmet(interaction: SkillItemOnItemInteraction): Boolean {
        val player = interaction.player
        if (player.inventory.transaction { remove(FISHBOWL_ID, 1); add(FISHBOWL_HELMET_ID, 1) }) {
            player.skills.gainXp(60, Skill.CRAFTING)
            player.ui.message("You chisel the fishbowl into a helmet.")
        }
        return true
    }

    // Legacy routed this through ContentActions.queueProductionSelection (an amount-picker
    // interface) - matching the same single-click-single-craft simplification this plugin's own
    // cutGem() already established for the identical legacy flow (gem cutting used the same
    // queueProductionSelection picker in Crafting.kt and was simplified the same way).
    //
    // Minor accepted deviation: legacy's wool+X branch fired for ANY item X, showing "You cannot
    // string this item with wool!" for anything that wasn't a real amulet. This plugin registers
    // itemOnItem per-amulet-id (the only shape the DSL supports without a wildcard primitive), so
    // wool+non-amulet no longer shows that message - it just falls through unhandled. A cosmetic
    // feedback-message gap only, not a functional/reachable-content regression.
    private fun stringAmulet(interaction: SkillItemOnItemInteraction): Boolean {
        val player = interaction.player
        val amuletId = if (interaction.itemUsed == BALL_OF_WOOL_ITEM_ID) interaction.otherItem else interaction.itemUsed
        val strungId = unstrungToStrungAmuletIds.getValue(amuletId)
        if (player.inventory.transaction { remove(amuletId, 1); remove(BALL_OF_WOOL_ITEM_ID, 1); add(strungId, 1) }) {
            player.skills.gainXp(60, Skill.CRAFTING)
            player.ui.message("You put the wool onto the ${player.inventory.itemName(strungId).lowercase()}.")
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
