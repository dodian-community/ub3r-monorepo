package net.dodian.uber.game.skill.crafting

import net.dodian.uber.game.skill.runtime.action.SkillActionRequest
import net.dodian.uber.game.skill.runtime.action.SkillActionState
import net.dodian.uber.game.skill.runtime.action.ActionStopReason

data class CraftingRequest(
    val mode: CraftingMode,
    val selectedItemId: Int = -1,
    val productId: Int = -1,
    val amount: Int = 0,
    val requiredLevel: Int = 0,
    val experience: Int = 0,
) : SkillActionRequest

enum class CraftingMode {
    LEATHER,
    SPINNING,
    SHAFTING,
}

data class CraftingState(
    val mode: CraftingMode,
    val selectedItemId: Int = -1,
    val productId: Int = -1,
    val remaining: Int = 0,
    val requiredLevel: Int = 0,
    val experience: Int = 0,
    override val active: Boolean = true,
    override val startedCycle: Long = 0L,
    override val stopReason: ActionStopReason? = null,
    override val targetRef: String? = null,
) : SkillActionState

data class GoldJewelryRequest(
    val interfaceId: Int,
    val slot: Int,
    val amount: Int,
)

data class GoldJewelryState(
    val selectedIndex: Int,
    val selectedSlot: Int,
    val remaining: Int,
)

data class GoldJewelryProduct(
    val index: Int,
    val slot: Int,
    val productId: Int,
    val requiredLevel: Int,
    val experience: Int,
)

data class TanningRequest(
    val hideType: Int,
    val amount: Int,
)

data class TanningDefinition(
    val hideType: Int,
    val hideId: Int,
    val leatherId: Int,
    val coinCost: Int,
)

data class HideDefinition(
    val itemId: Int,
    val experience: Int,
    val glovesId: Int,
    val glovesLevel: Int,
    val chapsId: Int,
    val chapsLevel: Int,
    val bodyId: Int,
    val bodyLevel: Int,
)

data class GemDefinition(
    val uncutId: Int,
    val cutId: Int,
    val requiredLevel: Int,
    val experience: Int,
    val animationId: Int,
)

data class OrbDefinition(
    val orbId: Int,
    val staffId: Int,
    val requiredLevel: Int,
    val experience: Int,
)

object CraftingData {
    @JvmField
    val hideDefinitions: List<HideDefinition> = listOf(
        HideDefinition(1745, 97, 1065, 50, 1099, 54, 1135, 58),
        HideDefinition(2505, 158, 2487, 62, 2493, 66, 2499, 70),
        HideDefinition(2507, 246, 2489, 73, 2495, 76, 2501, 79),
        HideDefinition(2509, 372, 2491, 82, 2497, 85, 2503, 88),
    )

    @JvmField
    val gemDefinitions: List<GemDefinition> = listOf(
        GemDefinition(1623, 1607, 20, 50, 888),
        GemDefinition(1621, 1605, 27, 68, 889),
        GemDefinition(1619, 1603, 34, 85, 887),
        GemDefinition(1617, 1601, 43, 108, 886),
        GemDefinition(1631, 1615, 55, 137, 885),
        GemDefinition(6571, 6573, 67, 168, 2717),
        GemDefinition(1625, 1609, 1, 15, 890),
        GemDefinition(1627, 1611, 13, 20, 891),
        GemDefinition(1629, 1613, 16, 25, 892),
    )

    @JvmField
    val orbDefinitions: List<OrbDefinition> = listOf(
        OrbDefinition(571, 1395, 51, 450),
        OrbDefinition(575, 1399, 56, 500),
        OrbDefinition(569, 1393, 61, 550),
        OrbDefinition(573, 1397, 66, 600),
    )

    @JvmStatic
    fun hideDefinition(index: Int): HideDefinition? = hideDefinitions.getOrNull(index)

    @JvmStatic
    fun findHideDefinition(itemId: Int): HideDefinition? = hideDefinitions.firstOrNull { it.itemId == itemId }

    @JvmStatic
    fun findGemDefinition(uncutId: Int): GemDefinition? = gemDefinitions.firstOrNull { it.uncutId == uncutId }

    @JvmStatic
    fun findOrbDefinition(orbId: Int): OrbDefinition? = orbDefinitions.firstOrNull { it.orbId == orbId }
}

object GoldJewelryDefinitions {
    @JvmField
    val blanks: Array<IntArray> =
        arrayOf(
            intArrayOf(-1, 1647, 1647, 1647, 1647, 1647, 1647),
            intArrayOf(-1, 1666, 1666, 1666, 1666, 1666, 1666),
            intArrayOf(-1, 1685, 1685, 1685, 1685, 1685, 1685),
        )

    @JvmField
    val interfaceSlots: IntArray = intArrayOf(4233, 4245, 4257, 79)

    @JvmField
    val requiredGemItems: IntArray = intArrayOf(-1, 1607, 1605, 1603, 1601, 1615, 6573)

    @JvmField
    val blackFrames: IntArray = intArrayOf(-1, -1, -1)

    @JvmField
    val frameSizes: IntArray = intArrayOf(100, 75, 120, 11067)

    @JvmField
    val requiredMoulds: IntArray = intArrayOf(1592, 1597, 1595, 11065)

    @JvmField
    val strungAmulets: IntArray = intArrayOf(1692, 1694, 1696, 1698, 1700, 1702, 6581)

    @JvmField
    val jewelryByGroup: Array<IntArray> =
        arrayOf(
            intArrayOf(1635, 1637, 1639, 1641, 1643, 1645, 6575),
            intArrayOf(1654, 1656, 1658, 1660, 1662, 1664, 6577),
            intArrayOf(1673, 1675, 1677, 1679, 1681, 1683, 6579),
            intArrayOf(11069, 11072, 11076, 11085, 11092, 11115, 11130),
        )

    @JvmField
    val levelsByGroup: Array<IntArray> =
        arrayOf(
            intArrayOf(5, 20, 27, 34, 43, 55, 67),
            intArrayOf(6, 22, 29, 40, 56, 72, 82),
            intArrayOf(8, 23, 31, 50, 70, 80, 90),
            intArrayOf(7, 24, 30, 42, 58, 74, 84),
        )

    @JvmField
    val experienceByGroup: Array<IntArray> =
        arrayOf(
            intArrayOf(15, 40, 55, 70, 85, 100, 115),
            intArrayOf(20, 55, 60, 75, 90, 105, 120),
            intArrayOf(30, 65, 70, 85, 100, 150, 165),
            intArrayOf(25, 60, 65, 80, 95, 110, 125),
        )

    private val products: List<GoldJewelryProduct> =
        buildList {
            levelsByGroup.forEachIndexed { index, levels ->
                levels.forEachIndexed { slot, level ->
                    add(
                        GoldJewelryProduct(
                            index = index,
                            slot = slot,
                            productId = jewelryByGroup[index][slot],
                            requiredLevel = level,
                            experience = experienceByGroup[index][slot] * 10,
                        ),
                    )
                }
            }
        }

    @JvmStatic
    fun product(index: Int, slot: Int): GoldJewelryProduct? = products.firstOrNull { it.index == index && it.slot == slot }

    @JvmStatic
    fun findProductByAmulet(amuletId: Int): GoldJewelryProduct? =
        products.firstOrNull { it.index == 2 && it.productId == amuletId }
}

object TanningDefinitions {
    private val definitions =
        listOf(
            TanningDefinition(hideType = 0, hideId = 1739, leatherId = 1741, coinCost = 50),
            TanningDefinition(hideType = 2, hideId = 1753, leatherId = 1745, coinCost = 1000),
            TanningDefinition(hideType = 3, hideId = 1751, leatherId = 2505, coinCost = 2000),
            TanningDefinition(hideType = 4, hideId = 1749, leatherId = 2507, coinCost = 5000),
            TanningDefinition(hideType = 5, hideId = 1747, leatherId = 2509, coinCost = 10000),
        )

    @JvmStatic
    fun find(hideType: Int): TanningDefinition? = definitions.firstOrNull { it.hideType == hideType }
}
