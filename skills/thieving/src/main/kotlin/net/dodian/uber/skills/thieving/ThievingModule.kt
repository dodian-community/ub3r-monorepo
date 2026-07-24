package net.dodian.uber.skills.thieving

import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.skills.SkillEquipmentSlot
import net.dodian.uber.game.api.plugin.skills.SkillNpcInteraction
import net.dodian.uber.game.api.plugin.skills.SkillObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.SkillPluginDefinition
import net.dodian.uber.game.api.plugin.skills.manifest
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.api.SkillModuleDescriptor
import net.dodian.uber.skills.runtime.TomlRecordReader

data class ThievingTargetDef(
    val name: String,
    val targetType: String,
    val id: Int,
    val level: Int,
    val experience: Int,
    val lootItem: Int,
    val lootAmount: Int,
)

object ThievingModule : SkillPlugin {
    val descriptor = SkillModuleDescriptor("skill.thieving", "Thieving")
    val targets: List<ThievingTargetDef> by lazy { loadTargets() }

    // Pyramid Plunder's room/obstacle objects. The old minigame implementation (Thieving.kt /
    // PyramidPlunder) was orphaned by this module's migration; re-deriving its obstacle/urn/tomb
    // state machine against the clean SkillPlayer API is out of scope here, so these objects get
    // an honest placeholder instead of fabricated "steal" behavior on what are really obstacles.
    private val plunderObjectIds = intArrayOf(
        20275, 20277, 20931, 20932, 26580,
        26600, 26601, 26602, 26603, 26604, 26605, 26606, 26607, 26608, 26609, 26610, 26611, 26612, 26613,
        26616, 26618, 26619, 26620, 26621, 26622, 26623, 26624, 26625, 26626,
    )

    private val npcIds by lazy { targets.filter { it.targetType == "npc" }.map { it.id }.toIntArray() }

    // Stalls and chests share the same simple steal action; both live on the same click option
    // as their respective real client-defined object (varies per object), so the handler is
    // registered on both options 1 and 2 for these ids.
    private val stealIds by lazy { targets.filter { it.targetType == "stall" || it.targetType == "chest" }.map { it.id }.toIntArray() }

    // Market stalls (e.g. the tea stall) only expose their steal option at option 2.
    private val marketStallIds by lazy { targets.filter { it.targetType == "market_stall" }.map { it.id }.toIntArray() }

    // Object 375 is reused at two unrelated real-world locations for two distinct one-off reward
    // chests (legacy content.objects.impl.thieving.ChestObjects); 378 is the empty/looted state
    // it swaps to afterwards, and just reports the chest as empty (matches legacy exactly).
    private const val SPECIAL_CHEST_OBJECT_ID = 375
    private const val EMPTY_CHEST_OBJECT_ID = 378
    private const val YANILLE_CHEST_X = 2593
    private const val YANILLE_CHEST_Y = 3108
    private const val LEGENDS_CHEST_X = 2733
    private const val LEGENDS_CHEST_Y = 3374
    private const val THIEVING_XP_BONUS_HEAD_ITEM = 2631

    override val definition: SkillPluginDefinition = skillPlugin("Thieving", Skill.THIEVING) {
        if (npcIds.isNotEmpty()) {
            npcClick(preset = PolicyPreset.GATHERING, option = 2, *npcIds) { interaction ->
                pickpocketNpc(interaction)
            }
        }
        if (stealIds.isNotEmpty()) {
            objectClick(preset = PolicyPreset.GATHERING, option = 1, *stealIds) { interaction ->
                stealFromTarget(interaction)
            }
            objectClick(preset = PolicyPreset.GATHERING, option = 2, *stealIds) { interaction ->
                stealFromTarget(interaction)
            }
        }
        if (marketStallIds.isNotEmpty()) {
            objectClick(preset = PolicyPreset.GATHERING, option = 2, *marketStallIds) { interaction ->
                stealFromTarget(interaction)
            }
        }
        objectClick(preset = PolicyPreset.GATHERING, option = 1, SPECIAL_CHEST_OBJECT_ID) { interaction ->
            openSpecialChest(interaction)
        }
        objectClick(preset = PolicyPreset.GATHERING, option = 2, SPECIAL_CHEST_OBJECT_ID) { interaction ->
            openSpecialChest(interaction)
        }
        objectClick(preset = PolicyPreset.GATHERING, option = 1, EMPTY_CHEST_OBJECT_ID) { interaction ->
            interaction.player.ui.message("This chest is empty!")
            true
        }
        objectClick(preset = PolicyPreset.GATHERING, option = 2, EMPTY_CHEST_OBJECT_ID) { interaction ->
            interaction.player.ui.message("This chest is empty!")
            true
        }
        objectClick(preset = PolicyPreset.GATHERING, option = 1, *plunderObjectIds) { interaction ->
            pyramidPlunderPlaceholder(interaction)
        }
        objectClick(preset = PolicyPreset.GATHERING, option = 2, *plunderObjectIds) { interaction ->
            pyramidPlunderPlaceholder(interaction)
        }
    }

    override val contentManifest: ContentModuleManifest = definition.manifest(
        id = descriptor.id,
        owner = "gameplay",
        version = descriptor.version,
        maturity = ContentMaturity.STABLE,
    )

    private fun pickpocketNpc(interaction: SkillNpcInteraction): Boolean {
        val player = interaction.player
        val target = targets.firstOrNull { it.id == interaction.npc.id } ?: return false
        if (player.skills.current(Skill.THIEVING) < target.level) {
            player.ui.message("You need level ${target.level} thieving to pickpocket this target.")
            return true
        }
        player.actions.animate(881)
        player.inventory.transaction { add(target.lootItem, target.lootAmount) }
        player.skills.gainXp(target.experience, Skill.THIEVING)
        player.actions.triggerRandomEvent(target.experience)
        player.ui.message("You pickpocket the ${target.name}.")
        return true
    }

    private fun stealFromTarget(interaction: SkillObjectInteraction): Boolean {
        val player = interaction.player
        val target = targets.firstOrNull { it.id == interaction.target.id } ?: return false
        if (player.skills.current(Skill.THIEVING) < target.level) {
            player.ui.message("You need level ${target.level} thieving to steal from this.")
            return true
        }
        player.actions.animate(832)
        player.inventory.transaction { add(target.lootItem, target.lootAmount) }
        player.skills.gainXp(target.experience, Skill.THIEVING)
        player.actions.triggerRandomEvent(target.experience)
        player.ui.message("You steal from the ${target.name}.")
        return true
    }

    // Legacy behavior (ChestObjects.onFirstClick, object 375 at two specific world tiles): each
    // location is a distinct one-off reward chest that swaps to the empty object (378) for a
    // while after being looted. Simplified from legacy: skips the yell broadcast on a rare drop
    // and the movement/animation-lock/random-event-tick cosmetics that have no clean-API
    // equivalent, but keeps the level gate, premium gate, loot tables, and head-slot xp bonus.
    private fun openSpecialChest(interaction: SkillObjectInteraction): Boolean {
        val player = interaction.player
        val position = interaction.position
        return when {
            position.x == YANILLE_CHEST_X && position.y == YANILLE_CHEST_Y -> {
                lootSpecialChest(
                    interaction = interaction,
                    requiredLevel = 70,
                    rareItems = intArrayOf(2577, 2579, 2631),
                    minCoins = 300,
                    coinsRange = 1200,
                    xpBonus = 300,
                    respawnTicks = 20,
                )
            }
            position.x == LEGENDS_CHEST_X && position.y == LEGENDS_CHEST_Y -> {
                if (!player.profile.premium) {
                    player.ui.message("Only premium members can open this chest.")
                    return true
                }
                lootSpecialChest(
                    interaction = interaction,
                    requiredLevel = 85,
                    rareItems = intArrayOf(1050, 2581, 2631),
                    minCoins = 500,
                    coinsRange = 2000,
                    xpBonus = 500,
                    respawnTicks = 25,
                )
            }
            else -> false
        }
    }

    private fun lootSpecialChest(
        interaction: SkillObjectInteraction,
        requiredLevel: Int,
        rareItems: IntArray,
        minCoins: Int,
        coinsRange: Int,
        xpBonus: Int,
        respawnTicks: Int,
    ): Boolean {
        val player = interaction.player
        if (player.skills.current(Skill.THIEVING) < requiredLevel) {
            player.ui.message("You must be level $requiredLevel thieving to open this chest.")
            return true
        }
        if (player.inventory.freeSlots() < 1) {
            player.ui.message("You need at least one free inventory slot!")
            return true
        }
        val rolledRare = player.random.chance(3, 1000)
        val committed = player.inventory.transaction {
            if (rolledRare) {
                add(rareItems[player.random.between(0, rareItems.size - 1)], 1)
            } else {
                add(995, player.random.between(minCoins, minCoins + coinsRange))
            }
        }
        if (!committed) return true
        player.world.replaceObject(interaction.target, EMPTY_CHEST_OBJECT_ID, respawnTicks)
        if (player.equipment.item(SkillEquipmentSlot.HEAD) == THIEVING_XP_BONUS_HEAD_ITEM) {
            player.skills.gainXp(xpBonus, Skill.THIEVING)
        }
        return true
    }

    private fun pyramidPlunderPlaceholder(interaction: SkillObjectInteraction): Boolean {
        interaction.player.ui.message("Pyramid Plunder is being restored - check back soon!")
        return true
    }

    private fun loadTargets(): List<ThievingTargetDef> =
        TomlRecordReader.readRecords("thieving/tables.toml", "target").map { row ->
            ThievingTargetDef(
                name = row.getValue("name"),
                targetType = row.getValue("target_type"),
                id = row.getValue("id").toInt(),
                level = row.getValue("level").toInt(),
                experience = row.getValue("experience").toInt(),
                lootItem = row.getValue("loot_item").toInt(),
                lootAmount = row.getValue("loot_amount").toInt(),
            )
        }
}
