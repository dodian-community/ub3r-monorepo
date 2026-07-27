package net.dodian.uber.skills.woodcutting

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

data class WoodcuttingTreeDef(
    val name: String,
    val objectIds: List<Int>,
    val logItemId: Int,
    val requiredLevel: Int,
    val experience: Int,
    val baseDelayMs: Long,
)

data class WoodcuttingAxeDef(
    val name: String,
    val itemId: Int,
    val requiredLevel: Int,
    val speedBonus: Double,
    val animationId: Int,
    val dragonTierBoostEligible: Boolean = false,
)

data class WoodcuttingSpot(val objectId: Int, val tree: WoodcuttingTreeDef)

/**
 * Plugin-owned reference implementation for tree-cutting, built on the same
 * [gatheringSpots] pipeline as Fishing — but object-targeted rather than npc-targeted,
 * which is what proves the pipeline generalizes beyond a single skill's shape. Content
 * is unchanged from the hand-written version this replaced: same 6 tree tiers, same
 * 9 axes, same "which axe animation/speed applies" resolution each cut.
 */
object WoodcuttingModule : SkillPlugin {
    val descriptor = SkillModuleDescriptor("skill.woodcutting", "Woodcutting")

    private const val ACTION_NAME = "woodcutting"
    private const val DRAGON_BOOST_CHANCE_ONE_IN = 8
    private const val DRAGON_BOOST_MS = 600.0
    private const val REST_THRESHOLD = 4
    private const val REST_CHANCE_ONE_IN = 20

    val trees: List<WoodcuttingTreeDef> by lazy { loadTrees() }
    val axes: List<WoodcuttingAxeDef> by lazy { loadAxes() }

    private val spots: List<WoodcuttingSpot> by lazy {
        trees.flatMap { tree -> tree.objectIds.map { objectId -> WoodcuttingSpot(objectId, tree) } }
    }

    override val definition: SkillPluginDefinition = skillPlugin("Woodcutting", Skill.WOODCUTTING) {
        gatheringSpots(spots, ACTION_NAME) { spot ->
            objectOption(spot.objectId, clickOption = 1, PolicyPreset.GATHERING)

            requireLevel(Skill.WOODCUTTING, spot.tree.requiredLevel) {
                "You need a woodcutting level of ${spot.tree.requiredLevel} to cut this tree."
            }
            requireFreeInventory("Your inventory is full!")
            requirement({ player -> resolveBestAxe(player) != null }) {
                "You need an axe in which you got the required woodcutting level for."
            }

            calculateDelayMs { player -> woodcuttingDelayMs(player, spot.tree) }
            restPolicy(minGathered = REST_THRESHOLD, chanceOneIn = REST_CHANCE_ONE_IN)

            onStart { player ->
                resolveBestAxe(player)?.let { player.actions.animate(it.animationId, 0) }
                player.ui.message("You swing your axe at the tree...")
            }
            onYield { player -> cutLog(player, spot.tree) }
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

    fun resolveBestAxe(player: SkillPlayer): WoodcuttingAxeDef? {
        val level = player.skills.current(Skill.WOODCUTTING)
        val equippedWeapon = player.equipment.item(SkillEquipmentSlot.WEAPON)
        return axes.firstOrNull { axe ->
            level >= axe.requiredLevel && (equippedWeapon == axe.itemId || player.inventory.contains(axe.itemId))
        }
    }

    private fun woodcuttingDelayMs(player: SkillPlayer, tree: WoodcuttingTreeDef): Long {
        val axe = resolveBestAxe(player) ?: return 1L
        val levelBonus = player.skills.current(Skill.WOODCUTTING) / 256.0
        val bonus = 1 + axe.speedBonus + levelBonus
        var timer = tree.baseDelayMs.toDouble()
        if (axe.dragonTierBoostEligible && player.random.chance(1, DRAGON_BOOST_CHANCE_ONE_IN)) {
            timer -= DRAGON_BOOST_MS
        }
        return (timer / bonus).toLong()
    }

    private fun cutLog(player: SkillPlayer, tree: WoodcuttingTreeDef) {
        resolveBestAxe(player)?.let { player.actions.animate(it.animationId, 0) }
        player.ui.message("You cut some ${player.inventory.itemName(tree.logItemId).lowercase()}")
        player.inventory.add(tree.logItemId, 1)
        player.actions.logGathering(tree.logItemId, 1, "Woodcutting")
        player.skills.gainXp(tree.experience, Skill.WOODCUTTING)
        player.actions.triggerRandomEvent(tree.experience)
    }

    private fun loadTrees(): List<WoodcuttingTreeDef> =
        TomlRecordReader.readRecords("woodcutting/trees.toml", "tree").map { row ->
            WoodcuttingTreeDef(
                name = row.getValue("name"),
                objectIds = row.getValue("objectIds").split(",").map { it.trim().toInt() },
                logItemId = row.getValue("logItemId").toInt(),
                requiredLevel = row.getValue("requiredLevel").toInt(),
                experience = row.getValue("experience").toInt(),
                baseDelayMs = row.getValue("baseDelayMs").toLong(),
            )
        }

    private fun loadAxes(): List<WoodcuttingAxeDef> =
        TomlRecordReader.readRecords("woodcutting/axes.toml", "axe").map { row ->
            WoodcuttingAxeDef(
                name = row.getValue("name"),
                itemId = row.getValue("itemId").toInt(),
                requiredLevel = row.getValue("requiredLevel").toInt(),
                speedBonus = row.getValue("speedBonus").toDouble(),
                animationId = row.getValue("animationId").toInt(),
                dragonTierBoostEligible = row["dragonTierBoostEligible"]?.toBoolean() ?: false,
            )
        }
}
