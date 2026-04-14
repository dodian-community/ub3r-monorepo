package net.dodian.uber.game.skill.thieving

import net.dodian.utilities.Range

enum class ThievingType {
    PICKPOCKETING,
    STALL_THIEVING,
    OTHER,
}

enum class ThievingDefinition(
    val entityId: Int,
    val requiredLevel: Int,
    val receivedExperience: Int,
    val item: IntArray,
    val itemAmount: Array<Range>,
    val itemChance: IntArray,
    val respawnTime: Int,
    val type: ThievingType,
) {
    FARMER(3086, 10, 800, intArrayOf(314), arrayOf(Range(2, 5)), intArrayOf(100), 0, ThievingType.PICKPOCKETING),
    MASTER_FARMER(3257, 70, 1200, intArrayOf(314), arrayOf(Range(4, 10)), intArrayOf(100), 0, ThievingType.PICKPOCKETING),
    CAGE(20873, 1, 150, intArrayOf(995), arrayOf(Range(20, 50)), intArrayOf(100), 0, ThievingType.OTHER),
    CAGE_ALT(20885, 1, 150, intArrayOf(995), arrayOf(Range(20, 50)), intArrayOf(100), 0, ThievingType.OTHER),
    BAKER_STALL(11730, 10, 1000, intArrayOf(2309), arrayOf(Range(1, 1)), intArrayOf(100), 12, ThievingType.STALL_THIEVING),
    FUR_STALL(11732, 40, 1800, intArrayOf(1751, 1753, 1739, 1759, 995), arrayOf(Range(1, 1), Range(1, 1), Range(1, 1), Range(1, 1), Range(150, 350)), intArrayOf(5, 10, 15, 20, 100), 25, ThievingType.STALL_THIEVING),
    SILVER_STALL(11734, 65, 2500, intArrayOf(2349, 2351, 2353, 2357, 2359, 995), arrayOf(Range(1, 1), Range(1, 1), Range(1, 1), Range(1, 1), Range(1, 1), Range(300, 600)), intArrayOf(5, 10, 15, 20, 25, 100), 25, ThievingType.STALL_THIEVING),
    SPICE_STALL(11733, 80, 4800, intArrayOf(215, 213, 209, 207, 203, 199), arrayOf(Range(1, 1), Range(1, 1), Range(1, 1), Range(1, 1), Range(1, 1), Range(1, 1)), intArrayOf(5, 10, 20, 35, 55, 100), 35, ThievingType.STALL_THIEVING),
    GEM_STALL(11731, 90, 5800, intArrayOf(1617, 1619, 1621, 1623, 995), arrayOf(Range(1, 1), Range(1, 1), Range(1, 1), Range(1, 1), Range(500, 850)), intArrayOf(2, 5, 8, 15, 100), 38, ThievingType.STALL_THIEVING);

    companion object {
        @JvmStatic
        fun forId(entityId: Int): ThievingDefinition? = values().firstOrNull { it.entityId == entityId }
    }
}

object ThievingObjectComponents {
    val stallObjects = intArrayOf(4877)
    val chestObjects = intArrayOf(375, 378, 6847, 20873, 20885, 11729, 11730, 11731, 11732, 11733, 11734)
    val plunderObjects = intArrayOf(
        20275, 20277, 20931, 20932, 26580,
        26600, 26601, 26602, 26603, 26604, 26605, 26606, 26607, 26608, 26609, 26610, 26611, 26612, 26613,
        26616, 26618, 26619, 26620, 26621, 26622, 26623, 26624, 26625, 26626,
    )
}

object ThievingData {
    val all: Array<ThievingDefinition> = ThievingDefinition.values()
}
