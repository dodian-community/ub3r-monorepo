package net.dodian.uber.skills.smithing

import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.skills.SkillItemOnNpcInteraction
import net.dodian.uber.game.api.plugin.skills.SkillItemOnObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillMagicOnItemInteraction
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

    private const val DRAGONFIRE_SHIELD_NPC_ID = 535
    private const val DRACONIC_VISAGE_ID = 1540
    private const val ANTI_DRAGON_SHIELD_ID = 11286
    private const val DRAGONFIRE_SHIELD_ID = 11284
    private const val DRAGONFIRE_SHIELD_COST = 1_500_000

    private const val ROCKSHELL_MENU_DIALOGUE_ID = 10000
    private const val HAMMER_ID = 2347
    private val rockshellItemIds = intArrayOf(6157, 6158, 6159, 6160, 6161)

    private const val SUPERHEAT_SPELL_ID = 1173
    private const val NATURE_RUNE_ID = 561
    private const val MAGIC_XP = 500
    private val superheatOreItemIds = intArrayOf(436, 438, 440, 444, 447, 449, 451)
    private val superheatGlassItemIds = intArrayOf(1781, 401, 1783)

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
        itemOnNpc(PolicyPreset.PRODUCTION, DRAGONFIRE_SHIELD_NPC_ID, itemIds = intArrayOf(DRACONIC_VISAGE_ID, ANTI_DRAGON_SHIELD_ID)) { interaction ->
            assembleDragonfireShield(interaction)
        }
        rockshellItemIds.indices.forEach { i ->
            (i + 1 until rockshellItemIds.size).forEach { j ->
                itemOnItem(PolicyPreset.DIALOGUE, rockshellItemIds[i], rockshellItemIds[j]) { interaction ->
                    interaction.player.ui.openDialogue(ROCKSHELL_MENU_DIALOGUE_ID)
                    true
                }
            }
        }
        playerOptionMenu(
            preset = PolicyPreset.PRODUCTION,
            dialogueId = ROCKSHELL_MENU_DIALOGUE_ID,
            title = "What would you like to make?",
            options = listOf("Head", "Body", "Legs", "Boots", "Gloves"),
            guard = { player ->
                when {
                    player.skills.current(Skill.SMITHING) < 60 -> "You need level 60 smithing to do this."
                    !player.inventory.contains(HAMMER_ID) -> "You need a hammer to handle this material."
                    else -> null
                }
            },
        ) { player, option -> craftRockshellPiece(player, option) }
        magicOnItem(PolicyPreset.PRODUCTION, *(superheatOreItemIds + superheatGlassItemIds), spellIds = intArrayOf(SUPERHEAT_SPELL_ID)) { interaction ->
            castSuperheat(interaction)
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

    /**
     * Assembles a dragonfire shield from a draconic visage + anti-dragon shield + 1.5m coins.
     * Reproduces the legacy `ItemOnNpcContentService` branch verbatim, including its message
     * wording. **Deliberately has no Smithing level check** (OSRS canon is 90) - confirmed with
     * the user during this port that the legacy no-gate behavior is preserved exactly, not fixed.
     */
    private fun assembleDragonfireShield(interaction: SkillItemOnNpcInteraction): Boolean {
        val player = interaction.player
        val otherItem = if (interaction.itemId == DRACONIC_VISAGE_ID) ANTI_DRAGON_SHIELD_ID else DRACONIC_VISAGE_ID
        if (!player.inventory.contains(otherItem)) {
            player.ui.message(if (interaction.itemId == DRACONIC_VISAGE_ID) "You need a anti-dragon shield!" else "You need a draconic visage!")
            return true
        }
        if (!player.inventory.contains(995, DRAGONFIRE_SHIELD_COST)) {
            player.ui.message("You need 1.5 million coins!")
            return true
        }
        val committed = player.inventory.transaction {
            remove(interaction.itemId)
            remove(otherItem)
            remove(995, DRAGONFIRE_SHIELD_COST)
            add(DRAGONFIRE_SHIELD_ID)
        }
        if (!committed) return true
        player.ui.message("Here you go. Your shield is done.")
        return true
    }

    /** Reproduces `RockshellDialogueOptionHandler`'s 5 legacy branches verbatim, including the
     * exact item-name ordering/punctuation of each "I need..." message. */
    private fun craftRockshellPiece(player: SkillPlayer, option: Int) {
        val inv = player.inventory
        when (option) {
            1 -> if (inv.contains(6161) && inv.contains(6159)) {
                if (player.inventory.transaction { remove(6159); remove(6161); add(6128) }) player.ui.message("I just made Rock-shell head.")
            } else {
                player.ui.message("I need the following items: ${inv.itemName(6161)} and ${inv.itemName(6159)}")
            }
            2 -> if (inv.contains(6157) && inv.contains(6159) && inv.contains(6161)) {
                if (player.inventory.transaction { remove(6157); remove(6159); remove(6161); add(6129) }) player.ui.message("I just made Rock-shell body.")
            } else {
                player.ui.message("I need the following items: ${inv.itemName(6161)}, ${inv.itemName(6159)} and ${inv.itemName(6157)}")
            }
            3 -> if (inv.contains(6159) && inv.contains(6157)) {
                if (player.inventory.transaction { remove(6157); remove(6159); add(6130) }) player.ui.message("I just made Rock-shell legs.")
            } else {
                player.ui.message("I need the following items: ${inv.itemName(6159)} and ${inv.itemName(6157)}")
            }
            4 -> if (inv.contains(6161) && inv.contains(6159)) {
                if (player.inventory.transaction { remove(6159); remove(6161); add(6145) }) player.ui.message("I just made Rock-shell boots.")
            } else {
                player.ui.message("I need the following items: ${inv.itemName(6161)} and ${inv.itemName(6159)}")
            }
            5 -> if (inv.contains(6161, 2)) {
                if (player.inventory.transaction { remove(6161, 2); add(6151) }) player.ui.message("I just made Rock-shell gloves.")
            } else {
                player.ui.message("I need two of ${inv.itemName(6161)}")
            }
        }
    }

    /** Reproduces `Superheat.cast`'s exact item dispatch. Registered items are always
     * recognized, so this always returns true (unrecognized items never reach this handler -
     * `PacketMagicService` falls back to its own "ores or glass material" message for those). */
    private fun castSuperheat(interaction: SkillMagicOnItemInteraction): Boolean {
        val player = interaction.player
        when (interaction.itemId) {
            1781, 401, 1783 -> superheatGlass(player)
            436, 438 -> superheatRecipe(player, 2349)
            440 -> if (player.inventory.contains(453, 2)) superheatRecipe(player, 2353) else superheatRecipe(player, 2351)
            444 -> superheatRecipe(player, 2357)
            447 -> superheatRecipe(player, 2359)
            449 -> superheatRecipe(player, 2361)
            451 -> superheatRecipe(player, 2363)
        }
        return true
    }

    private fun superheatRecipe(player: SkillPlayer, barId: Int) {
        val recipe = SmeltingRegistry.findRecipe(barId) ?: run {
            player.ui.message("You can only use this spell on ores or glass material!")
            player.world.castGraphic(85, 100)
            return
        }
        if (player.skills.current(Skill.SMITHING) < recipe.levelRequired) {
            player.ui.message("You need level ${recipe.levelRequired} smithing to do this!")
            return
        }
        if (!player.inventory.contains(NATURE_RUNE_ID)) {
            player.ui.message("You need 1 nature runes to cast this spell!")
            return
        }
        for (requirement in recipe.oreRequirements) {
            if (!player.inventory.contains(requirement.itemId, requirement.amount)) {
                player.ui.message(missingRequirementMessage(player, recipe))
                return
            }
        }

        player.actions.markMagicCastCycle()
        player.actions.animate(725, 0)
        player.world.castGraphic(148, 100)
        player.inventory.transaction {
            remove(NATURE_RUNE_ID, 1)
            recipe.oreRequirements.forEach { remove(it.itemId, it.amount) }
        }

        val success = recipe.successChancePercent >= 100 ||
            player.random.between(1, 100) <= recipe.successChancePercent + ((player.skills.current(Skill.SMITHING) + 1) / 4)
        if (success) {
            player.inventory.add(recipe.barId)
            player.skills.gainXp(recipe.experience, Skill.SMITHING)
            player.skills.gainXp(MAGIC_XP, Skill.MAGIC)
        } else if (recipe.failureMessage != null) {
            player.ui.message(recipe.failureMessage)
            player.skills.gainXp(MAGIC_XP, Skill.MAGIC)
        }
    }

    private fun superheatGlass(player: SkillPlayer) {
        if (!player.inventory.contains(NATURE_RUNE_ID, 3)) {
            player.ui.message("Need 3 nature runes to cast this spell on glass material!")
            return
        }
        if (!player.inventory.contains(1783) || (!player.inventory.contains(1781) && !player.inventory.contains(401))) {
            player.ui.message("You need atleast 1 bucket of sand along with seaweed or soda ash to cast this!")
            return
        }

        player.actions.markMagicCastCycle()
        player.actions.animate(725, 0)
        player.world.castGraphic(148, 100)
        val sandCount = player.inventory.amount(1783)
        val ashCount = player.inventory.amount(1781) + player.inventory.amount(401)
        val count = minOf(sandCount, ashCount)
        var moltenCount = 0
        repeat(count) {
            player.inventory.remove(1783, 1)
            player.inventory.remove(if (player.inventory.contains(1781)) 1781 else 401, 1)
            moltenCount++
            if (player.random.chance(30, 100)) moltenCount++
        }
        player.inventory.remove(NATURE_RUNE_ID, 3)
        repeat(moltenCount) {
            if (!player.inventory.add(1775)) player.world.dropItem(1775, 1)
        }
        player.skills.gainXp(count * 40, Skill.CRAFTING)
        player.skills.gainXp(MAGIC_XP, Skill.MAGIC)
    }

    private fun missingRequirementMessage(player: SkillPlayer, recipe: SmeltingRecipe): String = when (recipe.barId) {
        2349 -> "You need a tin and copper to do this!"
        2353 -> "You need a iron ore and 2 coal to do this!"
        2359 -> "You need a mithril ore and 3 coal to do this!"
        2361 -> "You need a adamantite ore and 4 coal to do this!"
        2363 -> "You need a runite ore and 6 coal to do this!"
        else -> {
            val missing = recipe.oreRequirements.firstOrNull { !player.inventory.contains(it.itemId, it.amount) }
            if (missing == null) "You can only use this spell on ores or glass material!"
            else "You need ${missing.amount} ${player.inventory.itemName(missing.itemId).lowercase()} to do this!"
        }
    }
}
