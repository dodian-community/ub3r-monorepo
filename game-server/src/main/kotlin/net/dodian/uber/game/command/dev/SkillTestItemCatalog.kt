package net.dodian.uber.game.command.dev

import net.dodian.uber.game.engine.systems.interaction.commands.*

import java.util.LinkedHashSet
import net.dodian.uber.skills.cooking.CookingModule
import net.dodian.uber.skills.crafting.CraftingModule
import net.dodian.uber.skills.firemaking.FiremakingModule
import net.dodian.uber.skills.farming.FarmingModule
import net.dodian.uber.skills.fishing.FishingModule
import net.dodian.uber.skills.fletching.FletchingModule
import net.dodian.uber.skills.herblore.HerbloreModule
import net.dodian.uber.skills.mining.MiningModule
import net.dodian.uber.skills.smithing.SmithingFrameDefinitions
import net.dodian.uber.skills.smithing.SmithingData
import net.dodian.uber.skills.smithing.SmeltingRegistry
import net.dodian.uber.skills.woodcutting.WoodcuttingModule

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
        WoodcuttingModule.axes.forEach { addIfValid(items, it.itemId) }
        WoodcuttingModule.trees.forEach { addIfValid(items, it.logItemId) }
        addAll(items, 590, 946)
        return items.toList()
    }

    private fun miningItems(): List<Int> {
        val items = LinkedHashSet<Int>()
        MiningModule.pickaxes.forEach { addIfValid(items, it.itemId) }
        MiningModule.rocks.forEach { addIfValid(items, it.oreItemId) }
        MiningModule.randomGemDropTable.forEach { addIfValid(items, it) }
        addAll(items, 1755, 1436)
        return items.toList()
    }

    private fun smithingItems(): List<Int> {
        val items = LinkedHashSet<Int>()
        addAll(items, 2347)
        SmeltingRegistry.recipes.forEach { addIfValid(items, it.barId) }
        for (row in SmithingFrameDefinitions.smithingFrame) {
            for (entry in row) {
                addIfValid(items, entry.itemId)
            }
        }
        MiningModule.rocks.forEach { addIfValid(items, it.oreItemId) }
        addAll(items, 436, 438, 440, 444, 447, 449, 451, 453)
        return items.toList()
    }

    private fun fletchingItems(): List<Int> {
        val items = LinkedHashSet<Int>()
        addAll(items, 946, 314, 52, 1777, 1779)
        FletchingModule.bowLogs.forEach {
            addIfValid(items, it.logItemId)
            addIfValid(items, it.unstrungShortbowId)
            addIfValid(items, it.shortbowId)
            addIfValid(items, it.unstrungLongbowId)
            addIfValid(items, it.longbowId)
        }
        FletchingModule.arrowRecipes.forEach {
            addIfValid(items, it.materialId)
            addIfValid(items, it.productId)
        }
        FletchingModule.dartRecipes.forEach {
            addIfValid(items, it.materialId)
            addIfValid(items, it.productId)
        }
        return items.toList()
    }

    private fun fishingItems(): List<Int> {
        val items = LinkedHashSet<Int>()
        FishingModule.spots.forEach {
            addIfValid(items, it.toolItemId)
            addIfValid(items, it.fishItemId)
        }
        CookingModule.recipes.forEach {
            addIfValid(items, it.rawItemId)
            addIfValid(items, it.cookedItemId)
            addIfValid(items, it.burntItemId)
        }
        addAll(items, 314, 21028)
        return items.toList()
    }

    private fun cookingItems(): List<Int> {
        val items = LinkedHashSet<Int>()
        CookingModule.recipes.forEach {
            addIfValid(items, it.rawItemId)
            addIfValid(items, it.cookedItemId)
            addIfValid(items, it.burntItemId)
        }
        return items.toList()
    }

    private fun firemakingItems(): List<Int> {
        val items = LinkedHashSet<Int>()
        addIfValid(items, 590)
        FiremakingModule.logs.forEach { addIfValid(items, it.itemId) }
        return items.toList()
    }

    private fun craftingItems(): List<Int> {
        val items = LinkedHashSet<Int>()
        addAll(items, 1733, 1734, 1755, 1592, 1595, 1597, 11065, 2357, 1783, 1781, 401, 1775, 1779, 1777, 1712)
        CraftingModule.gems.forEach {
            addIfValid(items, it.uncutId)
            addIfValid(items, it.cutId)
        }
        // Orb IDs and their charged staff results (static game data, no plugin list)
        addAll(items, 571, 569, 573, 575, 1395, 1393, 1397, 1399)
        CraftingModule.hides.forEach {
            addIfValid(items, it.itemId)
            addIfValid(items, it.glovesId)
            addIfValid(items, it.chapsId)
            addIfValid(items, it.bodyId)
        }
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
        HerbloreModule.herbs.forEach {
            addIfValid(items, it.grimyId)
            addIfValid(items, it.cleanId)
            addIfValid(items, it.unfinishedId)
        }
        HerbloreModule.potions.forEach {
            addIfValid(items, it.secondaryId)
            addIfValid(items, it.productId)
        }
        HerbloreModule.doses.forEach {
            addIfValid(items, it.oneDoseId)
            addIfValid(items, it.twoDoseId)
            addIfValid(items, it.threeDoseId)
            addIfValid(items, it.fourDoseId)
        }
        addAll(items, 6045, 245, 3049, 3051, 3000, 3002, 3004, 12695, 12697, 12699, 12701, 11730, 11731, 11732, 11733)
        return items.toList()
    }

    private fun farmingItems(): List<Int> {
        val items = LinkedHashSet<Int>()
        // All crop types (allotment, flower, herb, bush, fruit_tree, tree) from the plugin's TOML data
        FarmingModule.crops.forEach {
            addIfValid(items, it.seedId)
            addIfValid(items, it.harvestItem)
        }
        // Sapling potting items (oak/willow/maple/yew/magic tree seeds, plant pots, saplings)
        addAll(items,
            5312, 5313, 5314, 5315, 5316,  // tree seeds
            5358, 5359, 5360, 5361, 5362,  // planted pots
            5364, 5365, 5366, 5367, 5368,  // watered pots
            5370, 5371, 5372, 5373, 5374,  // saplings
        )
        // Compost item IDs
        addAll(items, 6032, 6034, 21483)  // compost, supercompost, ultracompost
        // Regular compost inputs
        addAll(items, 6055, 6010, 6014, 6020, 1793, 5986, 5504, 1955, 1963, 2108, 5970,
            1957, 1942, 1965, 1951, 2126, 753, 1779, 401, 249, 199, 251, 201, 253, 203, 255, 205, 257, 207)
        // Super compost inputs
        addAll(items, 2114, 5982, 5972, 5974, 5978, 5976, 231, 247, 239, 6018, 2998, 3049,
            261, 211, 263, 213, 3000, 3051, 265, 215, 2481, 2485, 267, 217, 269, 219, 259, 209)
        // Farming tools
        addAll(items, 1925, 952, 5341, 5343, 5325, 5354, 5350, 5329, 7409, 6036, 21622)
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
        net.dodian.uber.skills.thieving.ThievingModule.targets.forEach { target ->
            target.lootItems.forEach { addIfValid(items, it) }
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
