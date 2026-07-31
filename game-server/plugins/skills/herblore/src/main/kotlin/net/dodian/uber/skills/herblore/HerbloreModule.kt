package net.dodian.uber.skills.herblore

import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.skills.SkillItemInteraction
import net.dodian.uber.game.api.plugin.skills.SkillItemOnItemInteraction
import net.dodian.uber.game.api.plugin.skills.SkillPlayer
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
import net.dodian.uber.game.api.plugin.runtime.TomlRecordReader

data class HerbDefinition(val grimyId: Int, val cleanId: Int, val unfinishedId: Int, val level: Int, val cleaningXp: Int, val premiumOnly: Boolean)
data class PotionDefinition(val unfinishedId: Int, val secondaryId: Int, val productId: Int, val level: Int, val experience: Int, val premiumOnly: Boolean)
data class SupplyDefinition(val itemId: Int, val productId: Int, val amount: Int)
data class DoseDefinition(val oneDoseId: Int, val twoDoseId: Int, val threeDoseId: Int, val fourDoseId: Int)

object HerbloreModule : SkillPlugin {
    val descriptor = SkillModuleDescriptor("skill.herblore", "Herblore")
    val herbs: List<HerbDefinition> by lazy { loadHerbs() }
    val potions: List<PotionDefinition> by lazy { loadPotions() }
    val supplies: List<SupplyDefinition> by lazy { loadSupplies() }
    val doses: List<DoseDefinition> by lazy { loadDoses() }

    private const val EMPTY_VIAL_ID = 229

    private const val TORSTOL_HERB_ID = 269
    private const val UNFINISHED_TORSTOL_POTION_ID = 111
    private const val SUPER_ATTACK_POTION_ID = 2436
    private const val SUPER_STRENGTH_POTION_ID = 2440
    private const val SUPER_DEFENCE_POTION_ID = 2442
    private const val SUPER_COMBAT_POTION_ID = 12695
    private const val SUPER_COMBAT_LEVEL = 88
    private const val SUPER_COMBAT_XP = 600
    private val superCombatItemIds = intArrayOf(TORSTOL_HERB_ID, UNFINISHED_TORSTOL_POTION_ID, SUPER_ATTACK_POTION_ID, SUPER_STRENGTH_POTION_ID, SUPER_DEFENCE_POTION_ID)

    private const val COCONUT_ID = 2444
    private const val RANGING_POTION_ID = 5978
    private const val OVERLOAD_POTION_ID = 11730
    private const val OVERLOAD_LEVEL = 93
    private const val OVERLOAD_XP = 800
    private val overloadItemIds = intArrayOf(SUPER_COMBAT_POTION_ID, COCONUT_ID, RANGING_POTION_ID)

    // Zahur (npc 4753) / Jatix (npc 8532) NPC service - herb-cleaner/unfinished-potion-maker
    // batch grind and bulk potion decanting. Zahur/Jatix themselves stay in game-server as thin
    // NpcFamily dialogue-tree shells (their flavor-text talkTo trees are generic, reusable
    // engine dialogue infra, not herblore-specific) and call these functions directly.
    private const val ZAHUR_NPC_ID = 4753
    private const val COINS_ID = 995
    private const val GRIND_COST_PER_HERB = 200
    private const val UNFINISHED_POTION_COST = 1_000
    private const val NOTED_VIAL_OF_WATER_ID = 228
    private const val NOTED_EMPTY_VIAL_ID = 230
    private const val DECANT_DIALOGUE_ID = 20932

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
        doses.forEach { dose ->
            itemOnItem(PolicyPreset.PRODUCTION, dose.fourDoseId, EMPTY_VIAL_ID) { mixDose(it.player, dose.fourDoseId, EMPTY_VIAL_ID, dose.twoDoseId, 2) }
            itemOnItem(PolicyPreset.PRODUCTION, dose.threeDoseId, dose.threeDoseId) { mixDose(it.player, dose.threeDoseId, dose.threeDoseId, dose.fourDoseId, 1, dose.twoDoseId) }
            itemOnItem(PolicyPreset.PRODUCTION, dose.threeDoseId, dose.twoDoseId) { mixDose(it.player, dose.threeDoseId, dose.twoDoseId, dose.fourDoseId, 1, dose.oneDoseId) }
            itemOnItem(PolicyPreset.PRODUCTION, dose.twoDoseId, EMPTY_VIAL_ID) { mixDose(it.player, dose.twoDoseId, EMPTY_VIAL_ID, dose.oneDoseId, 2) }
            itemOnItem(PolicyPreset.PRODUCTION, dose.twoDoseId, dose.twoDoseId) { mixDose(it.player, dose.twoDoseId, dose.twoDoseId, dose.fourDoseId, 1, EMPTY_VIAL_ID) }
            itemOnItem(PolicyPreset.PRODUCTION, dose.oneDoseId, dose.oneDoseId) { mixDose(it.player, dose.oneDoseId, dose.oneDoseId, dose.twoDoseId, 1, EMPTY_VIAL_ID) }
            itemOnItem(PolicyPreset.PRODUCTION, dose.oneDoseId, dose.twoDoseId) { mixDose(it.player, dose.oneDoseId, dose.twoDoseId, dose.threeDoseId, 1, EMPTY_VIAL_ID) }
            itemOnItem(PolicyPreset.PRODUCTION, dose.oneDoseId, dose.threeDoseId) { mixDose(it.player, dose.oneDoseId, dose.threeDoseId, dose.fourDoseId, 1, EMPTY_VIAL_ID) }
        }
        superCombatItemIds.indices.forEach { i ->
            (i + 1 until superCombatItemIds.size).forEach { j ->
                itemOnItem(PolicyPreset.PRODUCTION, superCombatItemIds[i], superCombatItemIds[j]) { mixSuperCombat(it.player) }
            }
        }
        overloadItemIds.indices.forEach { i ->
            (i + 1 until overloadItemIds.size).forEach { j ->
                itemOnItem(PolicyPreset.PRODUCTION, overloadItemIds[i], overloadItemIds[j]) { mixOverload(it.player) }
            }
        }
        npcClick(PolicyPreset.DIALOGUE, option = 3, ZAHUR_NPC_ID) { openDecantMenu(it.player); true }
        npcClick(PolicyPreset.DIALOGUE, option = 4, ZAHUR_NPC_ID) { openHerbCleanerMenu(it.player); true }
        playerOptionMenu(
            preset = PolicyPreset.DIALOGUE,
            dialogueId = DECANT_DIALOGUE_ID,
            title = "What should we decant it into?",
            options = listOf("Four dose", "Three dose", "Two dose", "One dose", "Nevermind"),
        ) { player, option ->
            when (option) {
                1 -> decantPotions(player, 4)
                2 -> decantPotions(player, 3)
                3 -> decantPotions(player, 2)
                4 -> decantPotions(player, 1)
                else -> player.ui.message("Nevermind, I do not need anything.")
            }
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

    /** Reproduces HerbloreItemCombinations.handleDoseMixing's per-shape item deletes/adds
     * exactly - instant, no production delay, matching the legacy synchronous behavior. */
    private fun mixDose(player: SkillPlayer, removeA: Int, removeB: Int, addPrimary: Int, addPrimaryAmount: Int, addSecondary: Int = -1): Boolean {
        player.inventory.transaction {
            remove(removeA, 1)
            remove(removeB, 1)
            add(addPrimary, addPrimaryAmount)
            if (addSecondary != -1) add(addSecondary, 1)
        }
        return true
    }

    /** Reproduces HerbloreItemCombinations.handle's super-combat branch exactly, including the
     * 269-preferred-over-111 tie-break and the exact legacy messages. This is a simplified,
     * non-canonical recipe (real OSRS mixes the torstol-unfinished-potion first) - preserved
     * as-is, not "fixed" to be more OSRS-accurate. */
    private fun mixSuperCombat(player: SkillPlayer): Boolean {
        val hasTorstol = player.inventory.contains(TORSTOL_HERB_ID) || player.inventory.contains(UNFINISHED_TORSTOL_POTION_ID)
        if (!hasTorstol || !player.inventory.contains(SUPER_ATTACK_POTION_ID) || !player.inventory.contains(SUPER_STRENGTH_POTION_ID) || !player.inventory.contains(SUPER_DEFENCE_POTION_ID)) {
            player.ui.message("You need a torstol herb or (unf) potion, super attack, strength and defence potion!")
            return true
        }
        if (player.skills.current(Skill.HERBLORE) < SUPER_COMBAT_LEVEL) {
            player.ui.message("You need level $SUPER_COMBAT_LEVEL herblore to mix a super combat potion!")
            return true
        }
        val torstolToRemove = if (!player.inventory.contains(TORSTOL_HERB_ID)) UNFINISHED_TORSTOL_POTION_ID else TORSTOL_HERB_ID
        val committed = player.inventory.transaction {
            remove(SUPER_ATTACK_POTION_ID, 1)
            remove(torstolToRemove, 1)
            remove(SUPER_STRENGTH_POTION_ID, 1)
            remove(SUPER_DEFENCE_POTION_ID, 1)
            add(SUPER_COMBAT_POTION_ID, 1)
        }
        if (committed) {
            player.skills.gainXp(SUPER_COMBAT_XP, Skill.HERBLORE)
            player.ui.message("You mix the ingredients together and made a super combat potion.")
        }
        return true
    }

    /** Reproduces HerbloreItemCombinations.handle's overload branch exactly. */
    private fun mixOverload(player: SkillPlayer): Boolean {
        if (!player.inventory.contains(SUPER_COMBAT_POTION_ID) || !player.inventory.contains(COCONUT_ID) || !player.inventory.contains(RANGING_POTION_ID)) {
            player.ui.message("You need a coconut, super combat potion and a ranging potion!")
            return true
        }
        if (player.skills.current(Skill.HERBLORE) < OVERLOAD_LEVEL) {
            player.ui.message("You need level $OVERLOAD_LEVEL herblore to mix an overload potion!")
            return true
        }
        val committed = player.inventory.transaction {
            remove(RANGING_POTION_ID, 1)
            remove(COCONUT_ID, 1)
            remove(SUPER_COMBAT_POTION_ID, 1)
            add(OVERLOAD_POTION_ID, 1)
        }
        if (committed) {
            player.skills.gainXp(OVERLOAD_XP, Skill.HERBLORE)
            player.ui.message("You mix the ingredients together and made an overload potion.")
        }
        return true
    }

    // --- Zahur: herb-cleaner / unfinished-potion-maker batch service ---------------------------
    // Reproduces legacy HerbloreNpcDialogue.openHerbCleaner/openUnfinishedPotionMaker +
    // Herblore.processBatch exactly. Both menus operate entirely on *noted* item ids.

    fun openHerbCleanerMenu(player: SkillPlayer) {
        val items = herbs.mapNotNull { herb ->
            val notedGrimy = player.inventory.notedItemId(herb.grimyId)
            if (notedGrimy > 0 && player.inventory.contains(notedGrimy)) notedGrimy else null
        }
        if (items.isEmpty()) {
            player.ui.message("You got no herbs for me to clean!")
            return
        }
        player.ui.itemListMenu(items) { notedGrimyId -> startGrindBatch(player, notedGrimyId) }
    }

    fun openUnfinishedPotionMakerMenu(player: SkillPlayer) {
        val items = herbs.mapNotNull { herb ->
            val notedClean = player.inventory.notedItemId(herb.cleanId)
            val notedUnfinished = player.inventory.notedItemId(herb.unfinishedId)
            if (notedClean > 0 && notedUnfinished > 0 && player.inventory.contains(notedClean)) notedUnfinished else null
        }
        if (items.isEmpty()) {
            player.ui.message("You got no herbs for me to make into unfinish potions!")
            return
        }
        player.ui.itemListMenu(items) { notedUnfinishedId -> startUnfinishedBatch(player, notedUnfinishedId) }
    }

    private fun startGrindBatch(player: SkillPlayer, notedGrimyId: Int) {
        player.amountPrompt.request { amount -> processGrindBatch(player, notedGrimyId, amount); true }
    }

    private fun processGrindBatch(player: SkillPlayer, notedGrimyId: Int, requestedAmount: Int) {
        val herb = herbs.firstOrNull { player.inventory.notedItemId(it.grimyId) == notedGrimyId } ?: return
        val notedCleanId = player.inventory.notedItemId(herb.cleanId)
        val coins = player.inventory.amount(COINS_ID)
        val amount = minOf(requestedAmount, coins / GRIND_COST_PER_HERB, player.inventory.amount(notedGrimyId))
        if (amount <= 0) {
            player.ui.message("You need 1 herb and 200 coins for me to grind it for you.")
            return
        }
        player.inventory.transaction {
            remove(COINS_ID, amount * GRIND_COST_PER_HERB)
            remove(notedGrimyId, amount)
            add(notedCleanId, amount)
        }
        player.ui.message("Here is your all of $amount ${player.inventory.itemName(notedGrimyId).lowercase()}")
        openHerbCleanerMenu(player)
    }

    private fun startUnfinishedBatch(player: SkillPlayer, notedUnfinishedId: Int) {
        player.amountPrompt.request { amount -> processUnfinishedBatch(player, notedUnfinishedId, amount); true }
    }

    private fun processUnfinishedBatch(player: SkillPlayer, notedUnfinishedId: Int, requestedAmount: Int) {
        if (!player.inventory.contains(NOTED_VIAL_OF_WATER_ID)) {
            player.ui.message("You need noted vial of water for me to do that!")
            return
        }
        val herb = herbs.firstOrNull { player.inventory.notedItemId(it.unfinishedId) == notedUnfinishedId } ?: return
        val notedCleanId = player.inventory.notedItemId(herb.cleanId)
        val coins = player.inventory.amount(COINS_ID)
        val vials = player.inventory.amount(NOTED_VIAL_OF_WATER_ID)
        val heldHerbs = player.inventory.amount(notedCleanId)
        val amount = minOf(requestedAmount, coins / UNFINISHED_POTION_COST, vials, heldHerbs)
        if (amount <= 0) {
            player.ui.message("You need atleast 1 herb, 1 vial of water and 1000 coins for me to turn it into a unfinish potion.")
            return
        }
        player.inventory.transaction {
            remove(COINS_ID, amount * UNFINISHED_POTION_COST)
            remove(NOTED_VIAL_OF_WATER_ID, amount)
            remove(notedCleanId, amount)
            add(notedUnfinishedId, amount)
        }
        player.ui.message("Here is your all of $amount ${player.inventory.itemName(notedUnfinishedId).lowercase()}")
        openUnfinishedPotionMakerMenu(player)
    }

    // --- Zahur/Jatix: bulk potion decanting -----------------------------------------------------
    // Reproduces legacy HerbloreNpcDialogue.decantPotions exactly, including its one inconsistency:
    // the leftover partial-dose potion (e.g. a 1-dose remainder from converting to 4-dose) is
    // granted as the RAW/unnoted item, unlike every other item this function touches (produced
    // target potions and empty vials are both noted). Confirmed by re-reading the legacy source
    // twice - preserved as-is, not "fixed", matching this session's established preserve-over-fix
    // default for pre-existing legacy oddities.

    fun openDecantMenu(player: SkillPlayer) {
        player.ui.openDialogue(DECANT_DIALOGUE_ID)
    }

    private fun decantPotions(player: SkillPlayer, dose: Int) {
        doses.forEach { def ->
            var potionAmount = 0L
            var vialAmount = 0L
            fun collect(rawId: Int, doseValue: Int) {
                val notedId = player.inventory.notedItemId(rawId)
                if (notedId <= 0) return
                val held = player.inventory.amount(notedId)
                if (held <= 0) return
                potionAmount += held.toLong() * doseValue
                vialAmount += held.toLong()
                player.inventory.remove(notedId, held)
            }
            collect(def.fourDoseId, 4)
            collect(def.threeDoseId, 3)
            collect(def.twoDoseId, 2)
            collect(def.oneDoseId, 1)

            val targetId = when (dose) {
                4 -> def.fourDoseId
                3 -> def.threeDoseId
                2 -> def.twoDoseId
                else -> def.oneDoseId
            }
            val notedTarget = player.inventory.notedItemId(targetId)
            if (notedTarget <= 0) return@forEach

            val producedAmount = (potionAmount / dose).toInt()
            val leftOverDoseAmount = (potionAmount % dose).toInt()
            val leftOverItemId = when (leftOverDoseAmount) {
                3 -> def.threeDoseId
                2 -> def.twoDoseId
                1 -> def.oneDoseId
                else -> -1
            }
            val emptyVials = (vialAmount - producedAmount - if (leftOverDoseAmount > 0) 1 else 0).toInt()
            val currentEmptyVials = player.inventory.amount(NOTED_EMPTY_VIAL_ID)

            if (producedAmount > 0 && !player.inventory.add(notedTarget, producedAmount)) {
                player.world.dropItem(notedTarget, producedAmount)
            }
            if (emptyVials > 0 && (emptyVials + currentEmptyVials) < 1000) {
                if (!player.inventory.add(NOTED_EMPTY_VIAL_ID, emptyVials)) {
                    player.world.dropItem(NOTED_EMPTY_VIAL_ID, emptyVials)
                }
            } else if (emptyVials < 0) {
                player.inventory.remove(NOTED_EMPTY_VIAL_ID, -emptyVials)
            }
            if (leftOverItemId > 0) {
                if (!player.inventory.add(leftOverItemId, 1)) {
                    player.world.dropItem(leftOverItemId, 1)
                }
            }
        }
        player.ui.message("Enjoy your decanted potions ${player.profile.name}")
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

    private fun loadDoses(): List<DoseDefinition> = TomlRecordReader.readRecords(RESOURCE, "dose").mapIndexed { index, row ->
        DoseDefinition(row.int("one_dose_id", index), row.int("two_dose_id", index), row.int("three_dose_id", index), row.int("four_dose_id", index))
    }.distinctByOrFail("dose") { it.fourDoseId }

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
