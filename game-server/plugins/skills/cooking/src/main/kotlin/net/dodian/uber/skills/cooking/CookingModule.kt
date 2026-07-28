package net.dodian.uber.skills.cooking

import net.dodian.uber.game.api.content.ContentAttributeKey
import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.skills.SkillActionHandle
import net.dodian.uber.game.api.plugin.skills.SkillPlayer
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.SkillPluginDefinition
import net.dodian.uber.game.api.plugin.skills.SkillValidationResult
import net.dodian.uber.game.api.plugin.skills.manifest
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.skill.runtime.action.ActionStopReason
import net.dodian.uber.game.skill.runtime.action.CycleSignal
import net.dodian.uber.game.skill.runtime.action.productionAction
import net.dodian.uber.skills.api.SkillModuleDescriptor
import net.dodian.uber.skills.api.SkillMultiAction
import net.dodian.uber.skills.api.SkillMultiConfig
import net.dodian.uber.skills.api.SkillMultiEntry
import net.dodian.uber.skills.api.skillRecipe
import net.dodian.uber.game.api.plugin.runtime.TomlRecordReader

data class CookingRecipeDef(
    val rawItemId: Int,
    val cookedItemId: Int,
    val burntItemId: Int,
    val requiredLevel: Int,
    val experience: Int,
    val burnRollBase: Int,
)

/** Complete plugin-owned reference implementation for range/fire cooking. */
object CookingModule : SkillPlugin {
    val descriptor = SkillModuleDescriptor("skill.cooking", "Cooking")

    private const val ACTION_DELAY_TICKS = 3
    private const val COOK_ANIMATION_ID = 883
    private const val SEAWEED_ITEM_ID = 401
    private const val ASH_ITEM_ID = 1781
    private const val COOKING_RANGE_OBJECT_ID = 26181
    private const val CHEFS_HAT_ITEM_ID = 1949
    private const val COOKING_GAUNTLETS_ITEM_ID = 775
    private const val HEAD_SLOT = 0
    private const val HANDS_SLOT = 9
    // Current-cache loc mappings: Lumbridge (114), current world range (26181), and the
    // mapped cooking-range variant (4172).  The old 2728/2781 ids are now a bunkbed/null.
    private val rangeObjectIds = intArrayOf(114, 4172, 26181)

    private val activeActionKey = ContentAttributeKey<SkillActionHandle>("skill.cooking", "activeAction")
    private val remainingKey = ContentAttributeKey<Int>("skill.cooking", "remaining")

    val recipes: List<CookingRecipeDef> by lazy { loadRecipes() }

    override val definition: SkillPluginDefinition = skillPlugin("Cooking", Skill.COOKING) {
        itemOnObject(preset = PolicyPreset.PRODUCTION, objectIds = rangeObjectIds) { interaction ->
            if (interaction.objectId == COOKING_RANGE_OBJECT_ID && interaction.itemId == SEAWEED_ITEM_ID) {
                burnSeaweedToAsh(interaction.player)
            } else {
                interaction.player.world.anchor(interaction.position)
                startCooking(interaction.player, interaction.itemId, interaction.player.inventory.amount(interaction.itemId))
            }
            true
        }
    }

    override val contentManifest: ContentModuleManifest = definition.manifest(
        id = descriptor.id,
        owner = "gameplay",
        version = descriptor.version,
        maturity = ContentMaturity.STABLE,
    )

    private fun burnSeaweedToAsh(player: SkillPlayer) {
        val amount = player.inventory.amount(SEAWEED_ITEM_ID)
        val committed = amount > 0 && player.inventory.transaction { remove(SEAWEED_ITEM_ID, amount); add(ASH_ITEM_ID, amount) }
        if (committed) player.ui.message("You burn all your seaweed into ashes.")
    }

    fun firstCookableInInventory(player: SkillPlayer): Int =
        recipes.map { it.rawItemId }.firstOrNull { player.inventory.amount(it) > 0 } ?: -1

    /** Kept for callers that intentionally request a bounded batch; range use cooks the full stack. */
    fun promptQuantity(player: SkillPlayer, itemId: Int): Boolean {
        val recipe = recipes.firstOrNull { it.rawItemId == itemId } ?: return false
        val available = player.inventory.amount(itemId)
        if (available <= 0) return false
        if (available == 1) {
            startCooking(player, itemId, 1)
            return true
        }
        val skillRecipe = skillRecipe("cooking.recipe.$itemId", recipe.cookedItemId) {
            material(itemId)
            requirement(recipe.requiredLevel)
            experience(recipe.experience)
        }
        val config = SkillMultiConfig(
            key = "cooking.single.$itemId",
            verb = "cook",
            action = SkillMultiAction.COOK,
            entries = listOf(SkillMultiEntry(skillRecipe)),
        )
        return player.production.open(config) { selection -> startCooking(player, itemId, selection.amount) }
    }

    fun startCooking(player: SkillPlayer, itemId: Int, amount: Int) {
        if (amount <= 0) return
        val recipe = recipes.firstOrNull { it.rawItemId == itemId } ?: return
        stopAction(player)
        player.attributes.put(remainingKey, amount)
        val handle = productionAction("cooking") {
            delay(ACTION_DELAY_TICKS)
            requirements {
                item(itemId, 1, "You are out of fish")
                requirement { p ->
                    if (p.skills.current(Skill.COOKING) >= recipe.requiredLevel) {
                        SkillValidationResult.ok()
                    } else {
                        SkillValidationResult.failed(
                            "You need ${recipe.requiredLevel} cooking to cook the ${p.inventory.itemName(itemId).lowercase()}.",
                        )
                    }
                }
            }
            onCycleSignal { cookOnce(this, itemId, recipe) }
            onStop {
                player.attributes.remove(remainingKey)
                player.attributes.remove(activeActionKey)
            }
        }.start(player)
        if (handle == null) {
            player.attributes.remove(remainingKey)
            return
        }
        player.attributes.put(activeActionKey, handle)
    }

    fun stopAction(player: SkillPlayer, reason: ActionStopReason = ActionStopReason.USER_INTERRUPT) {
        player.attributes.get(activeActionKey)?.cancel(reason)
        player.attributes.remove(activeActionKey)
    }

    private fun cookOnce(player: SkillPlayer, itemId: Int, recipe: CookingRecipeDef): CycleSignal {
        val remaining = player.attributes.get(remainingKey) ?: return CycleSignal.stop()
        if (remaining <= 0) return CycleSignal.stop(ActionStopReason.COMPLETED)

        var ran = recipe.burnRollBase - player.skills.current(Skill.COOKING)
        if (player.equipment.item(HANDS_SLOT) == COOKING_GAUNTLETS_ITEM_ID) ran -= 4
        if (player.equipment.item(HEAD_SLOT) == CHEFS_HAT_ITEM_ID) ran -= 4
        if (player.equipment.item(HANDS_SLOT) == COOKING_GAUNTLETS_ITEM_ID && player.equipment.item(HEAD_SLOT) == CHEFS_HAT_ITEM_ID) ran -= 2
        ran = ran.coerceIn(0, 100)
        val burn = player.random.between(1, 99) <= ran

        val itemName = player.inventory.itemName(itemId)
        val committed = player.inventory.transaction {
            remove(itemId, 1)
            add(if (burn) recipe.burntItemId else recipe.cookedItemId, 1)
        }
        if (!committed) return CycleSignal.stop(ActionStopReason.REQUIREMENT_FAILED)

        player.attributes.put(remainingKey, remaining - 1)
        player.actions.animate(COOK_ANIMATION_ID, 0)
        if (burn) {
            player.ui.message("You burn the $itemName")
        } else {
            player.ui.message("You cook the $itemName")
            player.skills.gainXp(recipe.experience, Skill.COOKING)
        }
        player.actions.triggerRandomEvent(recipe.experience)

        return if (remaining - 1 <= 0) CycleSignal.completeSuccess() else CycleSignal.success()
    }

    private fun loadRecipes(): List<CookingRecipeDef> =
        TomlRecordReader.readRecords("cooking/recipes.toml", "recipe").map { row ->
            CookingRecipeDef(
                rawItemId = row.getValue("rawItemId").toInt(),
                cookedItemId = row.getValue("cookedItemId").toInt(),
                burntItemId = row.getValue("burntItemId").toInt(),
                requiredLevel = row.getValue("requiredLevel").toInt(),
                experience = row.getValue("experience").toInt(),
                burnRollBase = row.getValue("burnRollBase").toInt(),
            )
        }
}
