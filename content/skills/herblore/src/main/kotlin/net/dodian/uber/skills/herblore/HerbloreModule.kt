package net.dodian.uber.skills.herblore

import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.skills.SkillItemInteraction
import net.dodian.uber.game.api.plugin.skills.SkillItemOnItemInteraction
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.SkillPluginDefinition
import net.dodian.uber.game.api.plugin.skills.manifest
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.api.plugin.skills.startProduction
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.api.SkillModuleDescriptor
import net.dodian.uber.skills.api.SkillMultiAction
import net.dodian.uber.skills.api.SkillMultiConfig
import net.dodian.uber.skills.api.SkillMultiEntry
import net.dodian.uber.skills.api.SkillRecipe
import net.dodian.uber.skills.api.skillRecipe
import net.dodian.uber.skills.runtime.TomlRecordReader

data class HerbDefinition(val grimyId: Int, val cleanId: Int, val unfinishedId: Int, val level: Int, val cleaningXp: Int, val premiumOnly: Boolean)
data class PotionDefinition(val unfinishedId: Int, val secondaryId: Int, val productId: Int, val level: Int, val experience: Int, val premiumOnly: Boolean)
data class SupplyDefinition(val itemId: Int, val productId: Int, val amount: Int)

object HerbloreModule : SkillPlugin {
    val descriptor = SkillModuleDescriptor("skill.herblore", "Herblore")
    val herbs: List<HerbDefinition> by lazy { loadHerbs() }
    val potions: List<PotionDefinition> by lazy { loadPotions() }
    val supplies: List<SupplyDefinition> by lazy { loadSupplies() }

    override val definition: SkillPluginDefinition = skillPlugin("Herblore", Skill.HERBLORE) {
        herbs.forEach { herb ->
            itemClick(PolicyPreset.PRODUCTION, 1, herb.grimyId) { clean(it, herb) }
            itemOnItem(PolicyPreset.PRODUCTION, herb.cleanId, UNFINISHED_VIAL) { openUnfinished(it, herb) }
        }
        potions.forEach { potion ->
            itemOnItem(PolicyPreset.PRODUCTION, potion.unfinishedId, potion.secondaryId) { openFinished(it, potion) }
        }
        supplies.forEach { supply ->
            itemClick(PolicyPreset.PRODUCTION, 1, supply.itemId) { unpack(it, supply) }
        }
    }

    override val contentManifest: ContentModuleManifest = definition.manifest(
        id = descriptor.id,
        owner = "gameplay",
        version = descriptor.version,
        maturity = ContentMaturity.STABLE,
    )

    private fun clean(interaction: SkillItemInteraction, herb: HerbDefinition): Boolean {
        val player = interaction.player
        if (herb.premiumOnly && !player.profile.premium) {
            player.ui.message("Need premium to clean this herb!")
            return true
        }
        if (player.skills.current(Skill.HERBLORE) < herb.level) {
            player.ui.message("You need level ${herb.level} herblore to clean this herb.")
            return true
        }
        if (player.inventory.transaction { removeAt(interaction.itemSlot, herb.grimyId); add(herb.cleanId) }) {
            player.skills.gainXp(herb.cleaningXp, Skill.HERBLORE)
            player.ui.message("You clean the ${player.inventory.itemName(herb.grimyId)}.")
        }
        return true
    }

    private fun openUnfinished(interaction: SkillItemOnItemInteraction, herb: HerbDefinition): Boolean {
        val recipe = skillRecipe("herblore.unfinished.${herb.unfinishedId}", herb.unfinishedId) {
            material(herb.cleanId); material(UNFINISHED_VIAL); requirement(herb.level); experience(herb.cleaningXp)
            animation(363); delay(1); if (herb.premiumOnly) premiumOnly()
            success("You mix the herb with the vial of water.")
        }
        return openRecipe(interaction, recipe)
    }

    private fun openFinished(interaction: SkillItemOnItemInteraction, potion: PotionDefinition): Boolean {
        val recipe = skillRecipe("herblore.potion.${potion.productId}", potion.productId) {
            material(potion.unfinishedId); material(potion.secondaryId); requirement(potion.level); experience(potion.experience)
            animation(363); delay(3); if (potion.premiumOnly) premiumOnly()
            success("You mix the ${interaction.player.inventory.itemName(potion.secondaryId)} into your potion.")
        }
        return openRecipe(interaction, recipe)
    }

    private fun openRecipe(interaction: SkillItemOnItemInteraction, recipe: SkillRecipe): Boolean {
        val player = interaction.player
        return player.production.open(
            SkillMultiConfig("${recipe.key}.menu", "mix", SkillMultiAction.MAKE, entries = listOf(SkillMultiEntry(recipe))),
        ) { selection -> player.startProduction(recipe, selection.amount, Skill.HERBLORE) }
    }

    private fun unpack(interaction: SkillItemInteraction, supply: SupplyDefinition): Boolean {
        val player = interaction.player
        if (!player.inventory.transaction { removeAt(interaction.itemSlot, supply.itemId); add(supply.productId, supply.amount) }) {
            player.ui.message("You need enough inventory space to open this pack.")
        }
        return true
    }

    private fun loadHerbs(): List<HerbDefinition> = TomlRecordReader.readRecords(RESOURCE, "herb").mapIndexed { index, row ->
        HerbDefinition(row.int("grimy_id", index), row.int("clean_id", index), row.int("unfinished_id", index), row.int("level", index), row.int("cleaning_xp", index), row.bool("premium_only"))
    }.distinctByOrFail("grimy herb") { it.grimyId }

    private fun loadPotions(): List<PotionDefinition> = TomlRecordReader.readRecords(RESOURCE, "potion").mapIndexed { index, row ->
        PotionDefinition(row.int("unfinished_id", index), row.int("secondary_id", index), row.int("product_id", index), row.int("level", index), row.int("experience", index), row.bool("premium_only"))
    }.distinctByOrFail("potion") { "${it.unfinishedId}:${it.secondaryId}" }

    private fun loadSupplies(): List<SupplyDefinition> = TomlRecordReader.readRecords(RESOURCE, "supply").mapIndexed { index, row ->
        SupplyDefinition(row.int("item_id", index), row.int("product_id", index), row.int("amount", index))
    }.distinctByOrFail("supply") { it.itemId }

    private fun Map<String, String>.int(field: String, index: Int): Int = get(field)?.toIntOrNull()?.takeIf { it >= 0 }
        ?: error("Invalid $RESOURCE field $field at record $index")
    private fun Map<String, String>.bool(field: String): Boolean = get(field)?.toBooleanStrictOrNull() ?: false
    private fun <T, K> List<T>.distinctByOrFail(label: String, selector: (T) -> K): List<T> {
        require(map(selector).distinct().size == size) { "$RESOURCE contains duplicate $label records" }
        return this
    }

    private const val RESOURCE = "herblore/recipes.toml"
    private const val UNFINISHED_VIAL = 227
}
