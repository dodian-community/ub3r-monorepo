package net.dodian.uber.game.skills.crafting

data class GoldJewelryProduct(
    val index: Int,
    val slot: Int,
    val productId: Int,
    val requiredLevel: Int,
    val experience: Int,
)

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
