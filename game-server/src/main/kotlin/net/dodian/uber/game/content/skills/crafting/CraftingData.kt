package net.dodian.uber.game.content.skills.crafting

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
