package net.dodian.uber.skills.mining

import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
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

data class MiningRockDef(
    val name: String,
    val objectIds: List<Int>,
    val requiredLevel: Int,
    val baseDelayMs: Long,
    val oreItemId: Int,
    val experience: Int,
    val restThreshold: Int,
    val randomGemEligible: Boolean = true,
)

data class MiningPickaxeDef(
    val name: String,
    val itemId: Int,
    val requiredLevel: Int,
    val speedBonus: Double,
    val animationId: Int,
    val dragonTierBoostEligible: Boolean = false,
)

data class MiningSpot(val objectId: Int, val rock: MiningRockDef)

/**
 * Plugin-owned reference implementation for mining rocks, built on the same
 * [gatheringSpots] pipeline as Woodcutting/Fishing. Content is unchanged from the
 * hand-written version this replaced: same 10 rocks, same 9 pickaxes, same random
 * gem chance (boosted by an amulet of glory), same per-rock rest threshold.
 */
object MiningModule : SkillPlugin {
    val descriptor = SkillModuleDescriptor("skill.mining", "Mining")

    private const val ACTION_NAME = "mining"
    private const val DRAGON_BOOST_CHANCE_ONE_IN = 8
    private const val DRAGON_BOOST_MS = 600.0
    private const val REST_CHANCE_ONE_IN = 20
    private const val NECK_SLOT = 2
    private const val GLORY_GEM_CHANCE_ONE_IN = 128
    private const val NORMAL_GEM_CHANCE_ONE_IN = 256
    private const val RUNE_ESSENCE_ITEM_ID = 1436
    val randomGemDropTable: IntArray = intArrayOf(1623, 1623, 1623, 1621, 1621, 1619, 1617)

    // Tzhaar cave bounding box (matches the legacy Player.positions.TZHAAR region);
    // mining is disabled there.
    private const val TZHAAR_MIN_X = 2397
    private const val TZHAAR_MAX_X = 2494
    private const val TZHAAR_MIN_Y = 5120
    private const val TZHAAR_MAX_Y = 5183

    val rocks: List<MiningRockDef> by lazy { loadRocks() }
    val pickaxes: List<MiningPickaxeDef> by lazy { loadPickaxes() }

    private val spots: List<MiningSpot> by lazy {
        rocks.flatMap { rock -> rock.objectIds.map { objectId -> MiningSpot(objectId, rock) } }
    }

    override val definition: SkillPluginDefinition = skillPlugin("Mining", Skill.MINING) {
        gatheringSpots(spots, ACTION_NAME) { spot ->
            objectOption(spot.objectId, clickOption = 1, PolicyPreset.GATHERING)

            requirement({ player -> !inTzhaarCave(player) }) { "You can not mine here or the Tzhaar's will be angry!" }
            requireLevel(Skill.MINING, spot.rock.requiredLevel) { "You need a mining level of ${spot.rock.requiredLevel} to mine this rock" }
            requireFreeInventory("Your inventory is full!")
            requirement({ player -> resolveBestPickaxe(player) != null }) {
                "You need a pickaxe in which you got the required mining level for."
            }

            calculateDelayMs { player -> miningDelayMs(player, spot.rock) }
            restPolicy(minGathered = spot.rock.restThreshold, chanceOneIn = REST_CHANCE_ONE_IN)

            onStart { player ->
                resolveBestPickaxe(player)?.let { player.actions.animate(it.animationId, 0) }
                player.ui.message("You swing your pick at the rock...")
            }
            onYield { player -> mineOre(player, spot.rock) }
        }
    }

    override val contentManifest: ContentModuleManifest = definition.manifest(
        id = descriptor.id,
        owner = "gameplay",
        version = descriptor.version,
        maturity = ContentMaturity.STABLE,
    )

    fun stopAction(player: SkillPlayer, reason: ActionStopReason = ActionStopReason.USER_INTERRUPT) {
        stopGathering(player, ACTION_NAME, reason)
    }

    fun resolveBestPickaxe(player: SkillPlayer): MiningPickaxeDef? {
        val level = player.skills.current(Skill.MINING)
        val equippedWeapon = player.equipment.item(SkillEquipmentSlot.WEAPON)
        return pickaxes.firstOrNull { pickaxe ->
            level >= pickaxe.requiredLevel && (pickaxe.itemId == equippedWeapon || player.inventory.contains(pickaxe.itemId))
        }
    }

    private fun inTzhaarCave(player: SkillPlayer): Boolean {
        val position = player.world.position
        return position.x in TZHAAR_MIN_X..TZHAAR_MAX_X && position.y in TZHAAR_MIN_Y..TZHAAR_MAX_Y
    }

    private fun miningDelayMs(player: SkillPlayer, rock: MiningRockDef): Long {
        val pickaxe = resolveBestPickaxe(player) ?: return 1L
        val levelBonus = player.skills.current(Skill.MINING) / 256.0
        val bonus = 1 + pickaxe.speedBonus + levelBonus
        var timer = rock.baseDelayMs.toDouble()
        if (pickaxe.dragonTierBoostEligible && player.random.chance(1, DRAGON_BOOST_CHANCE_ONE_IN)) {
            timer -= DRAGON_BOOST_MS
        }
        return (timer / bonus).toLong()
    }

    private fun mineOre(player: SkillPlayer, rock: MiningRockDef) {
        if (rock.oreItemId != RUNE_ESSENCE_ITEM_ID) {
            player.ui.message("You mine some ${player.inventory.itemName(rock.oreItemId).lowercase()}")
        }
        player.inventory.add(rock.oreItemId, 1)
        player.actions.logGathering(rock.oreItemId, 1, "Mining")
        player.skills.gainXp(rock.experience, Skill.MINING)
        player.actions.triggerRandomEvent(rock.experience)
        if (rock.randomGemEligible) {
            tryAwardRandomGem(player)
        }
        resolveBestPickaxe(player)?.let { player.actions.animate(it.animationId, 0) }
    }

    private fun tryAwardRandomGem(player: SkillPlayer) {
        if (player.inventory.freeSlots() < 1) return
        val chance = resolveRandomGemChanceOneIn(player)
        if (!player.random.chance(1, chance)) return
        val gem = randomGemDropTable[player.random.between(0, randomGemDropTable.size - 1)]
        player.inventory.add(gem, 1)
        player.actions.logGathering(gem, 1, "Mining")
        player.ui.message("You found a ${player.inventory.itemName(gem).lowercase()} inside the rock.")
    }

    private fun resolveRandomGemChanceOneIn(player: SkillPlayer): Int {
        val neckItemName = player.inventory.itemName(player.equipment.item(NECK_SLOT)).lowercase()
        return if (neckItemName.contains("glory")) GLORY_GEM_CHANCE_ONE_IN else NORMAL_GEM_CHANCE_ONE_IN
    }

    private fun loadRocks(): List<MiningRockDef> =
        TomlRecordReader.readRecords("mining/rocks.toml", "rock").map { row ->
            MiningRockDef(
                name = row.getValue("name"),
                objectIds = row.getValue("objectIds").split(",").map { it.trim().toInt() },
                requiredLevel = row.getValue("requiredLevel").toInt(),
                baseDelayMs = row.getValue("baseDelayMs").toLong(),
                oreItemId = row.getValue("oreItemId").toInt(),
                experience = row.getValue("experience").toInt(),
                restThreshold = row.getValue("restThreshold").toInt(),
                randomGemEligible = row["randomGemEligible"]?.toBoolean() ?: true,
            )
        }

    private fun loadPickaxes(): List<MiningPickaxeDef> =
        TomlRecordReader.readRecords("mining/pickaxes.toml", "pickaxe").map { row ->
            MiningPickaxeDef(
                name = row.getValue("name"),
                itemId = row.getValue("itemId").toInt(),
                requiredLevel = row.getValue("requiredLevel").toInt(),
                speedBonus = row.getValue("speedBonus").toDouble(),
                animationId = row.getValue("animationId").toInt(),
                dragonTierBoostEligible = row["dragonTierBoostEligible"]?.toBoolean() ?: false,
            )
        }
}
