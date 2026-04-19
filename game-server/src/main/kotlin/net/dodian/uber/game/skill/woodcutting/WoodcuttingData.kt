package net.dodian.uber.game.skill.woodcutting

import net.dodian.cache.objects.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.skill.runtime.action.ActionStopReason
import net.dodian.uber.game.skill.runtime.action.SkillActionState

data class WoodcuttingState(
    val treeObjectId: Int,
    val treePosition: Position,
    val objectData: GameObjectData?,
    override val startedCycle: Long,
    val resourcesGathered: Int,
    override val active: Boolean = true,
    override val stopReason: ActionStopReason? = null,
    override val targetRef: String? = null,
) : SkillActionState

enum class TreeDef(
    val objectIds: IntArray,
    val logItemId: Int,
    val requiredLevel: Int,
    val experience: Int,
    val baseDelayMs: Long,
) {
    NORMAL(
        objectIds = intArrayOf(
            1276, 1277, 1278, 1279, 1280, 1330, 1332, 2409, 3033, 3034, 3035, 3036, 3879, 3881, 3882, 3883,
            1315, 1316, 1318, 1319,
            1282, 1283, 1284, 1285, 1286, 1287, 1289, 1290, 1291, 1365, 1383, 1384, 5902, 5903, 5904,
        ),
        logItemId = 1511,
        requiredLevel = 1,
        experience = 100,
        baseDelayMs = 1800L,
    ),
    OAK(
        objectIds = intArrayOf(1281, 3037),
        logItemId = 1521,
        requiredLevel = 15,
        experience = 165,
        baseDelayMs = 2400L,
    ),
    WILLOW(
        objectIds = intArrayOf(1308, 5551, 5552),
        logItemId = 1519,
        requiredLevel = 30,
        experience = 285,
        baseDelayMs = 3200L,
    ),
    MAPLE(
        objectIds = intArrayOf(1307, 4674),
        logItemId = 1517,
        requiredLevel = 45,
        experience = 425,
        baseDelayMs = 4600L,
    ),
    YEW(
        objectIds = intArrayOf(1309, 1754),
        logItemId = 1515,
        requiredLevel = 60,
        experience = 735,
        baseDelayMs = 5600L,
    ),
    MAGIC(
        objectIds = intArrayOf(1306, 1762),
        logItemId = 1513,
        requiredLevel = 75,
        experience = 1075,
        baseDelayMs = 8200L,
    ),
}

enum class AxeDef(
    val itemId: Int,
    val requiredLevel: Int,
    val speedBonus: Double,
    val animationId: Int,
    val dragonTierBoostEligible: Boolean = false,
) {
    THIRD_AGE(20011, 61, 0.8, 2846, true),
    DRAGON(6739, 61, 0.8, 2846, true),
    RUNE(1359, 41, 0.42, 867),
    ADAMANT(1357, 31, 0.33, 869),
    MITHRIL(1355, 21, 0.24, 871),
    BLACK(1361, 11, 0.15, 875),
    STEEL(1353, 6, 0.1, 875),
    IRON(1349, 1, 0.065, 877),
    BRONZE(1351, 1, 0.04, 879),
}

object WoodcuttingData {
    val treeByObjectId: Map<Int, TreeDef> =
        buildMap {
            TreeDef.values().forEach { tree ->
                tree.objectIds.forEach { objectId ->
                    put(objectId, tree)
                }
            }
        }

    val axeByItemId: Map<Int, AxeDef> = AxeDef.values().associateBy { it.itemId }

    val allTreeObjectIds: IntArray = treeByObjectId.keys.sorted().toIntArray()

    val axesDescending: List<AxeDef> = AxeDef.values().toList()
}
