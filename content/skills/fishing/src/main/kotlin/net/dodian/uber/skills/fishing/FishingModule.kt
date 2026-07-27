package net.dodian.uber.skills.fishing

import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.skills.GatheringSpotBuilder
import net.dodian.uber.game.api.plugin.skills.SkillEquipmentSlot
import net.dodian.uber.game.api.plugin.skills.SkillPlayer
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.SkillPluginDefinition
import net.dodian.uber.game.api.plugin.skills.gatheringSpots
import net.dodian.uber.game.api.plugin.skills.manifest
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.api.plugin.skills.stopGathering
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.skill.runtime.action.ActionStopReason
import net.dodian.uber.skills.api.SkillModuleDescriptor
import net.dodian.uber.skills.runtime.TomlRecordReader

data class FishingSpotDef(
    val index: Int,
    val npcId: Int,
    val clickOption: Int,
    val fishItemId: Int,
    val animationId: Int,
    val requiredLevel: Int,
    val baseDelayMs: Int,
    val toolItemId: Int,
    val experience: Int,
    val premiumOnly: Boolean = false,
    val featherConsumed: Boolean = false,
    val bigCatchItemId: Int = -1,
    val bigCatchRequiredLevel: Int = 0,
    val bigCatchBonusXp: Int = 0,
)

/**
 * Plugin-owned reference implementation for fishing spots, built on the shared
 * [gatheringSpots] pipeline (`:skills:runtime`). Content is unchanged from the
 * hand-written version this replaced: still one fish per spot/option, still just
 * spots.toml — the pipeline is generic so a future content pass (multiple catches
 * per option, separate tool/fish tables, etc.) is a data + `onYield` change, not a
 * new engine.
 */
object FishingModule : SkillPlugin {
    val descriptor = SkillModuleDescriptor("skill.fishing", "Fishing")

    private const val ACTION_NAME = "fishing"
    private const val HARPOON_ITEM_ID = 21028
    private const val HARPOON_LEVEL = 61
    private const val FEATHER_ITEM_ID = 314
    private const val REST_THRESHOLD = 4
    private const val REST_CHANCE_ONE_IN = 20

    val spots: List<FishingSpotDef> by lazy { loadSpots() }

    private lateinit var spotBuilders: List<GatheringSpotBuilder<FishingSpotDef>>

    override val definition: SkillPluginDefinition = skillPlugin("Fishing", Skill.FISHING) {
        spotBuilders = gatheringSpots(spots, ACTION_NAME) { spot ->
            npcOption(spot.npcId, spot.clickOption, PolicyPreset.GATHERING)

            requireFreeInventory("Not enough inventory space.")
            requireLevel(Skill.FISHING, spot.requiredLevel) { "You need level ${spot.requiredLevel} fishing to fish here." }
            requireTool(spot.toolItemId) { player -> "You need a ${player.inventory.itemName(spot.toolItemId).lowercase()} to fish here." }
            altTool { player -> hasHarpoon(player) }
            requirePremium(spot.premiumOnly) { "You need to be premium to fish from this spot!" }
            requireConsumable(FEATHER_ITEM_ID, enabled = spot.featherConsumed) { "You do not have any feathers." }

            repeatAnimation(spot.animationId, intervalMs = 1800L)
            calculateDelayMs { player -> catchDelayMs(player, spot) }
            restPolicy(minGathered = REST_THRESHOLD, chanceOneIn = REST_CHANCE_ONE_IN)

            onStart { player -> player.ui.message("You start fishing...") }
            onYield { player -> catchFish(player, spot) }
        }
    }

    override val contentManifest: ContentModuleManifest = definition.manifest(
        id = descriptor.id,
        owner = "gameplay",
        version = descriptor.version,
        maturity = ContentMaturity.STABLE,
    )

    fun attempt(player: SkillPlayer, npcId: Int, clickOption: Int) {
        val builder = spotBuilders.firstOrNull { it.targetId == npcId && it.clickOption == clickOption } ?: return
        builder.start(player, ACTION_NAME)
    }

    fun stopAction(player: SkillPlayer, reason: ActionStopReason = ActionStopReason.USER_INTERRUPT) {
        stopGathering(player, ACTION_NAME, reason)
    }

    private fun hasHarpoon(player: SkillPlayer): Boolean =
        player.skills.current(Skill.FISHING) >= HARPOON_LEVEL &&
            (player.equipment.item(SkillEquipmentSlot.WEAPON) == HARPOON_ITEM_ID || player.inventory.contains(HARPOON_ITEM_ID))

    private fun catchDelayMs(player: SkillPlayer, spot: FishingSpotDef): Long {
        val levelBonus = player.skills.current(Skill.FISHING) / 256.0
        val harpoon = hasHarpoon(player)
        val bonus = 1 + levelBonus + (if (harpoon) 0.2 else 0.0)
        var timer = spot.baseDelayMs.toDouble()
        if (harpoon && player.random.chance(1, 8)) timer -= 600
        return (timer / bonus).toLong()
    }

    private fun catchFish(player: SkillPlayer, spot: FishingSpotDef) {
        val bigCatch = spot.bigCatchItemId >= 0 &&
            player.skills.current(Skill.FISHING) >= spot.bigCatchRequiredLevel &&
            player.random.between(0, 6) < 3
        val itemId = if (bigCatch) spot.bigCatchItemId else spot.fishItemId
        val experience = if (bigCatch) spot.experience + spot.bigCatchBonusXp else spot.experience

        player.inventory.add(itemId, 1)
        player.actions.logGathering(itemId, 1, "Fishing")
        player.skills.gainXp(experience, Skill.FISHING)
        player.actions.triggerRandomEvent(experience)
        player.ui.message("You fish up some ${player.inventory.itemName(itemId).lowercase().replace("raw ", "")}.")
    }

    private fun loadSpots(): List<FishingSpotDef> =
        TomlRecordReader.readRecords("fishing/spots.toml", "spot").map { row ->
            FishingSpotDef(
                index = row.getValue("index").toInt(),
                npcId = row.getValue("npcId").toInt(),
                clickOption = row.getValue("clickOption").toInt(),
                fishItemId = row.getValue("fishItemId").toInt(),
                animationId = row.getValue("animationId").toInt(),
                requiredLevel = row.getValue("requiredLevel").toInt(),
                baseDelayMs = row.getValue("baseDelayMs").toInt(),
                toolItemId = row.getValue("toolItemId").toInt(),
                experience = row.getValue("experience").toInt(),
                premiumOnly = row["premiumOnly"]?.toBoolean() ?: false,
                featherConsumed = row["featherConsumed"]?.toBoolean() ?: false,
                bigCatchItemId = row["bigCatchItemId"]?.toInt() ?: -1,
                bigCatchRequiredLevel = row["bigCatchRequiredLevel"]?.toInt() ?: 0,
                bigCatchBonusXp = row["bigCatchBonusXp"]?.toInt() ?: 0,
            )
        }
}
