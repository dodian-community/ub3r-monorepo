package net.dodian.uber.game.content.commands.dev

import java.util.LinkedHashSet
import net.dodian.uber.game.Constants
import net.dodian.uber.game.model.player.skills.thieving.Thieving
import net.dodian.uber.game.skills.FarmingData
import net.dodian.uber.game.skills.mining.MiningData
import net.dodian.uber.game.skills.woodcutting.WoodcuttingData
import net.dodian.utilities.Utils

object SkillTestItemCatalog {
    private val categories: LinkedHashMap<String, List<Int>> =
        linkedMapOf(
            "prayer" to prayerItems(),
            "woodcutting" to woodcuttingItems(),
            "mining" to miningItems(),
            "smithing" to smithingItems(),
            "fletching" to fletchingItems(),
            "fishing" to fishingItems(),
            "cooking" to cookingItems(),
            "firemaking" to firemakingItems(),
            "crafting" to craftingItems(),
            "herblore" to herbloreItems(),
            "farming" to farmingItems(),
            "runecrafting" to runecraftingItems(),
            "thieving" to thievingItems(),
            "slayer" to slayerSupportItems(),
        )

    fun all(): List<Int> {
        val items = LinkedHashSet<Int>()
        categories.values.forEach { values -> values.forEach { addIfValid(items, it) } }
        return items.toList()
    }

    fun forSkill(raw: String): List<Int>? {
        val key = normalize(raw)
        val direct = categories[key]
        if (direct != null) {
            return direct
        }
        return when (key) {
            "wc" -> categories["woodcutting"]
            "mine" -> categories["mining"]
            "smith" -> categories["smithing"]
            "fish" -> categories["fishing"]
            "cook" -> categories["cooking"]
            "craft" -> categories["crafting"]
            "herb" -> categories["herblore"]
            "rc", "runecraft" -> categories["runecrafting"]
            "farm" -> categories["farming"]
            "thieve" -> categories["thieving"]
            else -> null
        }
    }

    private fun normalize(raw: String): String = raw.trim().lowercase().replace(" ", "").replace("_", "")

    private fun prayerItems(): List<Int> =
        linkedSetOf(526, 532, 536, 4830, 4832, 4834, 6729, 6812).toList()

    private fun woodcuttingItems(): List<Int> {
        val items = LinkedHashSet<Int>()
        WoodcuttingData.axesDescending.forEach { addIfValid(items, it.itemId) }
        WoodcuttingData.treeByObjectId.values.forEach { addIfValid(items, it.logItemId) }
        addAll(items, 590, 946)
        return items.toList()
    }

    private fun miningItems(): List<Int> {
        val items = LinkedHashSet<Int>()
        MiningData.pickaxesDescending.forEach { addIfValid(items, it.itemId) }
        MiningData.rocks.forEach { addIfValid(items, it.oreItemId) }
        MiningData.randomGemDropTable.forEach { addIfValid(items, it) }
        addAll(items, 1755, 1436)
        return items.toList()
    }

    private fun smithingItems(): List<Int> {
        val items = LinkedHashSet<Int>()
        addAll(items, 2347)
        Utils.smelt_bars.forEach { addIfValid(items, it[0]) }
        for (row in Constants.smithing_frame) {
            for (entry in row) {
                addIfValid(items, entry[0])
            }
        }
        MiningData.rocks.forEach { addIfValid(items, it.oreItemId) }
        addAll(items, 436, 438, 440, 444, 447, 449, 451, 453)
        return items.toList()
    }

    private fun fletchingItems(): List<Int> {
        val items = LinkedHashSet<Int>()
        addAll(items, 946, 314, 52, 1777, 1779)
        Constants.logs.forEach { addIfValid(items, it) }
        Constants.shortbows.forEach { addIfValid(items, it) }
        Constants.shortbow.forEach { addIfValid(items, it) }
        Constants.longbows.forEach { addIfValid(items, it) }
        Constants.longbow.forEach { addIfValid(items, it) }
        Constants.heads.forEach { addIfValid(items, it) }
        Constants.arrows.forEach { addIfValid(items, it) }
        Constants.darttip.forEach { addIfValid(items, it) }
        Constants.darts.forEach { addIfValid(items, it) }
        return items.toList()
    }

    private fun fishingItems(): List<Int> {
        val items = LinkedHashSet<Int>()
        Utils.fishTool.forEach { addIfValid(items, it) }
        Utils.fishId.forEach { addIfValid(items, it) }
        Utils.cookIds.forEach { addIfValid(items, it) }
        Utils.cookedIds.forEach { addIfValid(items, it) }
        Utils.burnId.forEach { addIfValid(items, it) }
        addAll(items, 314, 21028)
        return items.toList()
    }

    private fun cookingItems(): List<Int> {
        val items = LinkedHashSet<Int>()
        Utils.cookIds.forEach { addIfValid(items, it) }
        Utils.cookedIds.forEach { addIfValid(items, it) }
        Utils.burnId.forEach { addIfValid(items, it) }
        return items.toList()
    }

    private fun firemakingItems(): List<Int> {
        val items = LinkedHashSet<Int>()
        addIfValid(items, 590)
        Utils.woodcuttingLogs.forEach { addIfValid(items, it) }
        return items.toList()
    }

    private fun craftingItems(): List<Int> {
        val items = LinkedHashSet<Int>()
        addAll(items, 1733, 1734, 1755, 1592, 1595, 1597, 11065, 2357, 1783, 1781, 401, 1775, 1779, 1777, 1712)
        Utils.uncutGems.forEach { addIfValid(items, it) }
        Utils.cutGems.forEach { addIfValid(items, it) }
        Utils.orbs.forEach { addIfValid(items, it) }
        Utils.staves.forEach { addIfValid(items, it) }
        Constants.leathers.forEach { addIfValid(items, it) }
        Constants.gloves.forEach { addIfValid(items, it) }
        Constants.legs.forEach { addIfValid(items, it) }
        Constants.chests.forEach { addIfValid(items, it) }
        addAll(
            items,
            1741, 1745, 1747, 1749, 1751, 2505, 2507, 2509,
            1635, 1637, 1639, 1641, 1643, 1645, 6575,
            1654, 1656, 1658, 1660, 1662, 1664, 6577,
            1673, 1675, 1677, 1679, 1681, 1683, 6579,
            1692, 1694, 1696, 1698, 1700, 1702, 6581,
            11069, 11072, 11076, 11085, 11092, 11115, 11130,
            1607, 1605, 1603, 1601, 1615, 6573
        )
        return items.toList()
    }

    private fun herbloreItems(): List<Int> {
        val items = LinkedHashSet<Int>()
        addAll(items, 227, 228, 229, 233)
        Utils.grimy_herbs.forEach { addIfValid(items, it) }
        Utils.herbs.forEach { addIfValid(items, it) }
        Utils.herb_unf.forEach { addIfValid(items, it) }
        Utils.secondary.forEach { addIfValid(items, it) }
        Utils.finished.forEach { addIfValid(items, it) }
        Utils.pot_1_dose.forEach { addIfValid(items, it) }
        Utils.pot_2_dose.forEach { addIfValid(items, it) }
        Utils.pot_3_dose.forEach { addIfValid(items, it) }
        Utils.pot_4_dose.forEach { addIfValid(items, it) }
        addAll(items, 6045, 245, 3049, 3051, 3000, 3002, 3004, 12695, 12697, 12699, 12701, 11730, 11731, 11732, 11733)
        return items.toList()
    }

    private fun farmingItems(): List<Int> {
        val items = LinkedHashSet<Int>()
        FarmingData.allotmentPatch.values().forEach {
            addIfValid(items, it.seed)
            addIfValid(items, it.harvestItem)
        }
        FarmingData.flowerPatch.values().forEach {
            addIfValid(items, it.seed)
            addIfValid(items, it.harvestItem)
        }
        FarmingData.herbPatch.values().forEach {
            addIfValid(items, it.seed)
            addIfValid(items, it.harvestItem)
        }
        FarmingData.bushPatch.values().forEach {
            addIfValid(items, it.seed)
            addIfValid(items, it.harvestItem)
        }
        FarmingData.fruitTreePatch.values().forEach {
            addIfValid(items, it.seed)
            addIfValid(items, it.harvestItem)
        }
        FarmingData.treePatch.values().forEach {
            addIfValid(items, it.seed)
            addIfValid(items, it.harvestItem)
        }
        FarmingData.sapling.values().forEach {
            addIfValid(items, it.treeSeed)
            addIfValid(items, it.plantedId)
            addIfValid(items, it.waterId)
            addIfValid(items, it.saplingId)
        }
        FarmingData.compost.values().forEach { addIfValid(items, it.itemId) }
        FarmingData().regularCompostItems.forEach { addIfValid(items, it) }
        FarmingData().superCompostItems.forEach { addIfValid(items, it) }
        addAll(
            items,
            FarmingData().BUCKET, FarmingData().SPADE, FarmingData().RAKE, FarmingData().SEED_DIBBER, FarmingData().TROWEL,
            FarmingData().FILLED_PLANT_POT, FarmingData().EMPTY_PLANT_POT, FarmingData().SECATEURS, FarmingData().MAGIC_SECATEURS,
            FarmingData().PLANT_CURE, FarmingData().VOLCANIC_ASH
        )
        return items.toList()
    }

    private fun runecraftingItems(): List<Int> {
        val items = LinkedHashSet<Int>()
        addAll(items, 1436, 5509, 5510, 5512, 5514, 564, 565, 561)
        addAll(items, 1438, 1440, 1442, 1444, 1446, 1448, 1450, 1452, 1454, 1456, 1458, 1460, 1462)
        addAll(items, 5525, 5527, 5529, 5531, 5533, 5535, 5537, 5539, 5541, 5543, 5545, 5547, 5549)
        addAll(items, 5521, 5523, 5558, 5559, 5560, 5561, 5562, 5563, 5564, 5565, 5566, 5567, 5568)
        return items.toList()
    }

    private fun thievingItems(): List<Int> {
        val items = LinkedHashSet<Int>()
        Thieving.ThievingData.values().forEach { data ->
            data.item.forEach { addIfValid(items, it) }
        }
        return items.toList()
    }

    private fun slayerSupportItems(): List<Int> =
        linkedSetOf(4168, 8921, 11864, 1543, 1544, 1545, 2382, 2383, 989).toList()

    private fun addAll(set: LinkedHashSet<Int>, vararg values: Int) {
        values.forEach { addIfValid(set, it) }
    }

    private fun addIfValid(set: LinkedHashSet<Int>, value: Int) {
        if (value > 0) {
            set += value
        }
    }
}
