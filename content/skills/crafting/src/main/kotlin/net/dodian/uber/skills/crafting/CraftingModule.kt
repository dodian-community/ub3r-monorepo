package net.dodian.uber.skills.crafting

import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
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

    // Legacy spinning wheel objects (skill.crafting.Crafting.performSpin/spinDelayMs, orphaned by
    // the old content.objects.impl.crafting.SpinningWheelObjects registration that used to wire
    // these up). Flax takes priority over wool when both are carried, matching that behavior.
    private val spinningWheelObjectIds = intArrayOf(14889, 25824)
    private const val FLAX_ITEM_ID = 1779
    private const val BOW_STRING_ITEM_ID = 1777
    private const val FLAX_SPIN_EXPERIENCE = 50
    private const val WOOL_ITEM_ID = 1737
    private const val BALL_OF_WOOL_ITEM_ID = 1759
    private const val WOOL_SPIN_EXPERIENCE = 100

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
