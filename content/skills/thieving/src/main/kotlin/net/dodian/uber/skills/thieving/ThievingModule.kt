package net.dodian.uber.skills.thieving

import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.skills.SkillEquipmentSlot
import net.dodian.uber.game.api.plugin.skills.SkillNpcInteraction
import net.dodian.uber.game.api.plugin.skills.SkillObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillPlayer
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
    /** Parallel weighted-drop table: at [lootChances][i] (cumulative, out of 100), grant [lootItems][i]. */
    val lootItems: IntArray,
    val lootChances: IntArray,
    val lootMinAmounts: IntArray,
    val lootMaxAmounts: IntArray,
    /** Ticks the object stays swapped to the empty-stall id after a successful "stall" steal; 0 = no swap (npc/chest/market_stall targets). */
    val respawnTicks: Int,
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

    private val stallIds by lazy { targets.filter { it.targetType == "stall" }.map { it.id }.toIntArray() }
    private val cageIds = intArrayOf(20873, 20885)

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

    // Legacy Thieving.EMPTY_STALL_ID - what a "stall" target_type object swaps to after a
    // successful steal, for respawnTicks ticks, before reverting to the real stall object.
    private const val EMPTY_STALL_ID = 634
    private const val THIEVING_THROTTLE_KEY = "thieving.generic"

    override val definition: SkillPluginDefinition = skillPlugin("Thieving", Skill.THIEVING) {
        if (npcIds.isNotEmpty()) {
            npcClick(preset = PolicyPreset.GATHERING, option = 2, *npcIds) { interaction ->
                pickpocketNpc(interaction)
            }
        }
        // Exact legacy options: cages are first-click (the alternate cage also supports second),
        // while stalls are second-click only.  The remaining historical IDs were handled no-ops.
        objectClick(preset = PolicyPreset.GATHERING, option = 1, *cageIds) { stealFromTarget(it) }
        objectClick(preset = PolicyPreset.GATHERING, option = 2, 20885, *stallIds) { stealFromTarget(it) }
        objectClick(preset = PolicyPreset.GATHERING, option = 1, 6847) { true }
        objectClick(preset = PolicyPreset.GATHERING, option = 2, 11729, 4877) { true }
        objectClick(preset = PolicyPreset.GATHERING, option = 1, SPECIAL_CHEST_OBJECT_ID) { interaction ->
            openSpecialChest(interaction)
        }
        objectClick(preset = PolicyPreset.GATHERING, option = 2, EMPTY_CHEST_OBJECT_ID) { interaction ->
            interaction.player.ui.message("This chest is empty!")
            true
        }
    }

    override val contentManifest: ContentModuleManifest = definition.manifest(
        id = descriptor.id,
        owner = "gameplay",
        version = descriptor.version,
        maturity = ContentMaturity.STABLE,
    )

    /**
     * Rolls the weighted drop table (legacy Thieving.attempt's `if (rollChance < data.itemChance[i])`
     * scan) and picks an amount within that entry's range.
     */
    private fun rollLoot(target: ThievingTargetDef, player: SkillPlayer): Pair<Int, Int> {
        val roll = player.random.between(0, 99)
        val index = target.lootItems.indices.firstOrNull { roll < target.lootChances[it] } ?: target.lootItems.lastIndex
        return target.lootItems[index] to player.random.between(target.lootMinAmounts[index], target.lootMaxAmounts[index])
    }

    // Legacy per-tile stall face lookup (Thieving.attempt) - stalls have no orientation data of
    // their own, so the face the empty-stall replacement should render at is hardcoded per known
    // world tile, matching each stall's real placement.
    private fun stallFace(x: Int, y: Int): Int = when {
        (x == 2658 && y == 3297) || (x == 2663 && y == 3296) -> 0
        (x == 2655 && y == 3311) || (x == 2656 && y == 3302) -> 1
        (x == 2662 && y == 3314) || (x == 2657 && y == 3314) -> 2
        (x == 2667 && y == 3303) || (x == 2667 && y == 3310) -> 3
        else -> -1
    }

    private fun pickpocketNpc(interaction: SkillNpcInteraction): Boolean {
        val player = interaction.player
        val target = targets.firstOrNull { it.id == interaction.npc.id } ?: return false
        if (player.skills.current(Skill.THIEVING) < target.level) {
            player.ui.message("You need a thieving level of ${target.level} to steal from ${target.name.replace('_', ' ')}s.")
            return true
        }
        if (!player.actions.tryThrottle(THIEVING_THROTTLE_KEY, 2000L)) {
            return true
        }
        player.actions.animate(881)
        val (itemId, amount) = rollLoot(target, player)
        if (!player.inventory.transaction { add(itemId, amount) }) {
            player.ui.message("You don't have enough inventory space!")
            return true
        }
        player.actions.logGathering(itemId, amount, "Thieving")
        player.skills.gainXp(target.experience, Skill.THIEVING)
        player.actions.triggerRandomEvent(target.experience)
        player.ui.message("You receive ${article(player.inventory.itemName(itemId))} ${player.inventory.itemName(itemId).lowercase()}")
        return true
    }

    private fun stealFromTarget(interaction: SkillObjectInteraction): Boolean {
        val player = interaction.player
        val target = targets.firstOrNull { it.id == interaction.target.id } ?: return false
        if (player.skills.current(Skill.THIEVING) < target.level) {
            player.ui.message("You need a thieving level of ${target.level} to steal from ${target.name.replace('_', ' ')}s.")
            return true
        }
        if (!player.actions.tryThrottle(THIEVING_THROTTLE_KEY, 2000L)) {
            return true
        }
        val isStall = target.targetType == "stall"
        // Legacy: PICKPOCKETING/OTHER targets (npcs, chests, cages) use the pickpocket emote;
        // only STALL_THIEVING targets use the stall-steal emote.
        player.actions.animate(if (isStall) 832 else 881)
        val (itemId, amount) = rollLoot(target, player)
        if (!player.inventory.transaction { add(itemId, amount) }) {
            player.ui.message("You don't have enough inventory space!")
            return true
        }
        player.actions.logGathering(itemId, amount, "Thieving")
        player.skills.gainXp(target.experience, Skill.THIEVING)
        player.actions.triggerRandomEvent(target.experience)
        player.ui.message("You receive ${article(player.inventory.itemName(itemId))} ${player.inventory.itemName(itemId).lowercase()}")
        if (isStall && target.respawnTicks > 0) {
            val face = stallFace(interaction.position.x, interaction.position.y)
            if (face >= 0) {
                player.world.replaceObject(interaction.target, EMPTY_STALL_ID, target.respawnTicks, face = face)
            }
        }
        return true
    }

    // Legacy behavior (ChestObjects.onFirstClick, object 375 at two specific world tiles): each
    // location is a distinct one-off reward chest that swaps to the empty object (378) for a
    // while after being looted, with an orientation specific to that chest (the replacement has
    // no orientation of its own to infer it from). Restored to full legacy parity: loot messages,
    // an object-tile gfx, a server yell on the rare drop, a 1200ms per-chest throttle, the
    // level/premium/free-slot gates, the head-slot xp bonus, and the random-event trigger.
    private fun openSpecialChest(interaction: SkillObjectInteraction): Boolean {
        val player = interaction.player
        val position = interaction.position
        return when {
            // Legacy only gates the Yanille chest on plane 1; Legends has no plane check.
            position.x == YANILLE_CHEST_X && position.y == YANILLE_CHEST_Y && position.z == 1 -> {
                lootSpecialChest(
                    interaction = interaction,
                    chestName = "Yanille",
                    requiredLevel = 70,
                    rareItems = intArrayOf(2577, 2579, 2631),
                    minCoins = 300,
                    coinsRange = 1200,
                    xpBonus = 300,
                    respawnTicks = 20,
                    replacementType = 10,
                    replacementFace = 2,
                    throttleKey = "thieving.yanille_chest",
                    randomEventExperience = 900,
                )
            }
            position.x == LEGENDS_CHEST_X && position.y == LEGENDS_CHEST_Y -> {
                if (!player.profile.premium) {
                    player.world.resetPosition()
                    return true
                }
                lootSpecialChest(
                    interaction = interaction,
                    chestName = "Legends",
                    requiredLevel = 85,
                    rareItems = intArrayOf(1050, 2581, 2631),
                    minCoins = 500,
                    coinsRange = 2000,
                    xpBonus = 500,
                    respawnTicks = 25,
                    replacementType = 11,
                    replacementFace = -1,
                    throttleKey = "thieving.legends_chest",
                    randomEventExperience = 1500,
                )
            }
            else -> false
        }
    }

    private fun lootSpecialChest(
        interaction: SkillObjectInteraction,
        chestName: String,
        requiredLevel: Int,
        rareItems: IntArray,
        minCoins: Int,
        coinsRange: Int,
        xpBonus: Int,
        respawnTicks: Int,
        replacementType: Int,
        replacementFace: Int,
        throttleKey: String,
        randomEventExperience: Int,
    ): Boolean {
        val player = interaction.player
        if (player.skills.current(Skill.THIEVING) < requiredLevel) {
            player.ui.message("You must be level $requiredLevel thieving to open this chest")
            return true
        }
        if (player.inventory.freeSlots() < 1) {
            player.ui.message("You need atleast one free inventory slot!")
            return true
        }
        if (!player.actions.tryThrottle(throttleKey, 1200L)) {
            return true
        }
        val rolledRare = player.random.chance(3, 1000)
        var grantedItemId = -1
        var grantedAmount = 0
        val committed = player.inventory.transaction {
            if (rolledRare) {
                grantedItemId = rareItems[player.random.between(0, rareItems.size - 1)]
                grantedAmount = 1
            } else {
                grantedItemId = 995
                grantedAmount = player.random.between(minCoins, minCoins + coinsRange - 1)
            }
            add(grantedItemId, grantedAmount)
        }
        if (!committed) return true
        player.actions.logGathering(grantedItemId, grantedAmount, "Thieving")
        if (rolledRare) {
            val itemName = player.inventory.itemName(grantedItemId)
            player.ui.message("You have recieved a $itemName!")
            player.actions.yell("[Server] - ${player.profile.name} has just received from the $chestName chest a  $itemName")
        } else {
            player.ui.message("You find $grantedAmount coins inside the chest")
        }
        player.world.graphic(444, interaction.position)
        player.world.replaceObject(interaction.target, EMPTY_CHEST_OBJECT_ID, respawnTicks, type = replacementType, face = replacementFace)
        if (player.equipment.item(SkillEquipmentSlot.HEAD) == THIEVING_XP_BONUS_HEAD_ITEM) {
            player.skills.gainXp(xpBonus, Skill.THIEVING)
        }
        player.actions.triggerRandomEvent(randomEventExperience)
        return true
    }

    private fun article(itemName: String): String = when {
        itemName.startsWith("a", ignoreCase = true) || itemName.startsWith("e", ignoreCase = true) ||
            itemName.startsWith("i", ignoreCase = true) || itemName.startsWith("o", ignoreCase = true) ||
            itemName.startsWith("u", ignoreCase = true) -> if (itemName.endsWith("s", ignoreCase = true)) "some" else "an"
        itemName.endsWith("s", ignoreCase = true) -> "some"
        else -> "a"
    }

    private fun parseIntArray(value: String): IntArray = value.split(",").map { it.trim().toInt() }.toIntArray()

    private fun loadTargets(): List<ThievingTargetDef> =
        TomlRecordReader.readRecords("thieving/tables.toml", "target").map { row ->
            ThievingTargetDef(
                name = row.getValue("name"),
                targetType = row.getValue("target_type"),
                id = row.getValue("id").toInt(),
                level = row.getValue("level").toInt(),
                experience = row.getValue("experience").toInt(),
                lootItems = parseIntArray(row.getValue("loot_items")),
                lootChances = parseIntArray(row.getValue("loot_chances")),
                lootMinAmounts = parseIntArray(row.getValue("loot_min_amounts")),
                lootMaxAmounts = parseIntArray(row.getValue("loot_max_amounts")),
                respawnTicks = row["respawn_ticks"]?.toInt() ?: 0,
            )
        }
}
