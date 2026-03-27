package net.dodian.uber.game.content.skills.crafting

data class TanningDefinition(
    val hideType: Int,
    val hideId: Int,
    val leatherId: Int,
    val coinCost: Int,
)

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
