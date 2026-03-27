package net.dodian.uber.game.content.skills.guide

import net.dodian.uber.game.Constants
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.content.skills.fletching.FletchingDefinitions
import net.dodian.uber.game.content.skills.herblore.HerbloreDefinitions
import net.dodian.uber.game.content.skills.smithing.SmithingFrameDefinitions
import net.dodian.utilities.Utils

object SkillGuideDefinitions {
    private const val PREMIUM = " @red@(Premium only)"

    private val definitions: Map<Int, SkillGuideDefinition> =
        listOf(
            attack(),
            defence(),
            strength(),
            hitpoints(),
            ranged(),
            prayer(),
            magic(),
            thieving(),
            runecrafting(),
            fishing(),
            cooking(),
            crafting(),
            smithing(),
            agility(),
            woodcutting(),
            mining(),
            slayer(),
            firemaking(),
            herblore(),
            fletching(),
            farming(),
        ).associateBy { it.skillId }

    @JvmStatic
    fun find(skillId: Int): SkillGuideDefinition? = definitions[skillId]

    private fun attack() =
        skillGuide(Skill.ATTACK.id) {
            labels(8846 to "Attack", 8823 to "Defence", 8824 to "Range", 8827 to "Magic")
            pages { _, _ ->
                page(
                    names = arrayOf(
                        "Abyssal Whip", "Bronze", "Iron", "Steel", "Elemental battlestaff", "Mithril", "Adamant", "Rune",
                        "Wolfbane", "Keris", "Unholy book", "Unholy blessing", "Granite longsword", "Obsidian weapon",
                        "Dragon", "Verac's flail", "Torag's hammers", "Skillcape$PREMIUM",
                    ),
                    levels = arrayOf("1", "1", "1", "10", "20", "20", "30", "40", "40", "40", "45", "45", "50", "55", "60", "70", "70", "99"),
                    items = intArrayOf(4151, 1291, 1293, 1295, 1395, 1299, 1301, 1303, 2952, 10581, 3842, 20223, 21646, 6523, 1305, 4755, 4747, 9747),
                )
            }
        }

    private fun defence() =
        skillGuide(Skill.DEFENCE.id) {
            labels(8846 to "Attack", 8823 to "Defence", 8824 to "Range", 8827 to "Magic")
            pages { _, _ ->
                page(
                    names = arrayOf(
                        "Skeletal", "Bronze", "Iron", "Steel", "Mithril", "Splitbark", "Adamant", "Rune",
                        "Ancient blessing", "Granite", "Obsidian", "Dragon", "Barrows",
                        "Crystal shield (with 60 agility)", "Dragonfire shield", "Skillcape$PREMIUM",
                    ),
                    levels = arrayOf("1", "1", "1", "10", "20", "20", "30", "40", "45", "50", "55", "60", "70", "70", "75", "99"),
                    items = intArrayOf(6139, 1117, 1115, 1119, 1121, 3387, 1123, 1127, 20235, 10564, 21301, 3140, 4749, 4224, 11284, 9753),
                )
            }
        }

    private fun strength() =
        skillGuide(Skill.STRENGTH.id) {
            layout(hideComponents = intArrayOf(8825, 8813))
            labels(8846 to "Strength")
            pages { _, _ ->
                page(
                    names = arrayOf(
                        "Unholy book", "Unholy blessing", "War blessing", "Granite maul", "Obsidian maul",
                        "Dharok's greataxe", "Guthan's warspear", "Skillcape$PREMIUM",
                    ),
                    levels = arrayOf("45", "45", "45", "50", "55", "70", "70", "99"),
                    items = intArrayOf(3842, 20223, 20232, 4153, 6528, 4718, 4726, 9750),
                )
            }
        }

    private fun hitpoints() =
        skillGuide(Skill.HITPOINTS.id) {
            layout(hideComponents = intArrayOf(8825, 8813))
            labels(8846 to "Hitpoints")
            pages { _, _ ->
                page(
                    names = arrayOf(
                        "Shrimps (3 health)", "Chicken (3 health)", "Meat (3 health)", "Bread (5 health)", "Thin snail (7 health)",
                        "Trout (8 health)", "Salmon (10 health)", "Lobster (12 health)", "Swordfish (14 health)",
                        "Monkfish (16 health)$PREMIUM", "Shark (20 health)", "Sea Turtle (22 health)$PREMIUM", "Manta Ray (24 health)$PREMIUM",
                    ),
                    items = intArrayOf(315, 2140, 2142, 2309, 3369, 333, 329, 379, 373, 7946, 385, 397, 391),
                )
            }
        }

    private fun ranged() =
        skillGuide(Skill.RANGED.id) {
            layout(hideComponents = intArrayOf(8825))
            labels(8846 to "Bows", 8823 to "Armour", 8824 to "Misc")
            pages { _, child ->
                when (child) {
                    0 -> page(
                        names = arrayOf("Oak bow", "Willow bow", "Maple bow", "Yew bow", "Magic bow", "Crystal bow", "Karil's crossbow", "Seercull"),
                        levels = arrayOf("1", "20", "30", "40", "50", "65", "70", "75"),
                        items = intArrayOf(843, 849, 853, 857, 861, 4212, 4734, 6724),
                    )
                    1 -> page(
                        names = arrayOf(
                            "Leather", "Green dragonhide body (with 40 defence)", "Green dragonhide chaps", "Green dragonhide vambraces",
                            "Book of balance", "Peaceful blessing", "Honourable blessing", "Blue dragonhide body (with 40 defence)",
                            "Blue dragonhide chaps", "Blue dragonhide vambraces", "Red dragonhide body (with 40 defence)",
                            "Red dragonhide chaps", "Red dragonhide vambraces", "Black dragonhide body (with 40 defence)",
                            "Black dragonhide chaps", "Black dragonhide vambraces", "Karil (with 70 defence)", "Spined",
                        ),
                        levels = arrayOf("1", "40", "40", "40", "45", "45", "45", "50", "50", "50", "60", "60", "60", "70", "70", "70", "70", "75"),
                        items = intArrayOf(1129, 1135, 1099, 1065, 3844, 20226, 20229, 2499, 2493, 2487, 2501, 2495, 2489, 2503, 2497, 2491, 4736, 6133),
                    )
                    2 -> page(
                        names = arrayOf("Bronze arrow", "Iron arrow", "Steel arrow", "Mithril arrow", "Adamant arrow", "Rune arrow", "Dragon arrow", "Skillcape$PREMIUM"),
                        levels = arrayOf("1", "1", "10", "20", "30", "40", "60", "99"),
                        items = intArrayOf(882, 884, 886, 888, 890, 892, 11212, 9756),
                    )
                    else -> emptyPage()
                }
            }
        }

    private fun prayer() =
        skillGuide(Skill.PRAYER.id) {
            layout(hideComponents = intArrayOf(8825, 8813))
            labels(8846 to "Prayer")
            pages { _, _ ->
                page(
                    names = arrayOf(
                        "Thick Skin", "Burst of Strength", "Clarity of Thought", "Sharp Eye", "Mystic Will", "Rock Skin",
                        "Superhuman Strength", "Improved Reflexes", "Hawk Eye", "Mystic Lore", "Wolfbane", "Steel Skin",
                        "Ultimate Strength", "Incredible Reflexes", "Eagle Eye", "Mystic Might", "Protect from Magic",
                        "Protect from Missiles", "Protect from Melee", "Chivalry", "Piety", "Skillcape$PREMIUM",
                    ),
                    levels = arrayOf("5", "5", "5", "10", "10", "20", "20", "20", "25", "25", "25", "40", "40", "40", "45", "45", "55", "55", "55", "70", "80", "99"),
                    items = intArrayOf(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 2952, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 9759),
                )
            }
        }

    private fun magic() =
        skillGuide(Skill.MAGIC.id) {
            layout(hideComponents = intArrayOf(8825))
            labels(8846 to "Spells", 8823 to "Armor", 8824 to "Misc")
            pages { _, child ->
                when (child) {
                    0 -> page(
                        names = arrayOf(
                            "High Alch", "Smoke Rush", "Enchant Sapphire", "Shadow Rush", "Blood Rush", "Enchant Emerald",
                            "Ice Rush", "Smoke Burst", "Superheat", "Enchant Ruby", "Shadow Burst", "Enchant Diamond",
                            "Blood Burst", "Enchant Dragonstone", "Ice Burst", "Smoke Blitz", "Shadow Blitz", "Blood Blitz",
                            "Ice Blitz", "Smoke Barrage", "Enchant Onyx", "Shadow Barrage", "Blood Barrage", "Ice Barrage",
                        ),
                        levels = arrayOf("1", "1", "7", "10", "20", "27", "30", "40", "43", "49", "50", "57", "60", "68", "70", "74", "76", "80", "82", "86", "87", "88", "92", "94"),
                        items = intArrayOf(561, 565, 564, 565, 565, 564, 565, 565, 561, 564, 565, 564, 565, 564, 565, 565, 565, 565, 565, 565, 564, 565, 565, 565),
                        amounts = intArrayOf(1, 1, 10, 1, 1, 10, 1, 1, 1, 10, 1, 10, 1, 10, 1, 1, 1, 1, 1, 1, 10, 1, 1, 1),
                    )
                    1 -> page(
                        names = arrayOf("Blue Mystic", "White Mystic", "Splitbark (with 20 defence)", "Black Mystic", "Holy book", "Holy blessing", "Infinity", "Ahrim (with 70 defence)"),
                        levels = arrayOf("1", "20", "20", "35", "45", "45", "50", "70"),
                        items = intArrayOf(4089, 4109, 3385, 4099, 3840, 20220, 6918, 4708),
                    )
                    2 -> page(
                        names = arrayOf("Battlestaff", "Elemental battlestaff", "Zamorak staff", "Saradomin staff", "Guthix staff", "Ancient staff", "Obsidian staff", "Master wand", "Ahrim's staff", "Skillcape$PREMIUM"),
                        levels = arrayOf("1", "1", "10", "10", "10", "25", "40", "50", "70", "99"),
                        items = intArrayOf(1391, 1395, 2417, 2415, 2416, 4675, 6526, 6914, 4710, 9762),
                    )
                    else -> emptyPage()
                }
            }
        }

    private fun thieving() =
        skillGuide(Skill.THIEVING.id) {
            layout(hideComponents = intArrayOf(8825, 8813))
            labels(8846 to "Thieving")
            pages { _, _ ->
                page(
                    names = arrayOf("Cage", "Farmer", "Baker stall", "fur stall", "silver stall", "Master Farmer", "Yanille chest", "Spice Stall", "Legends chest$PREMIUM", "Gem Stall$PREMIUM"),
                    levels = arrayOf("1", "10", "10", "40", "65", "70", "70", "80", "85", "90"),
                    items = intArrayOf(4443, 3243, 2309, 1739, 2349, 5068, 6759, 199, 6759, 1623),
                )
            }
        }

    private fun runecrafting() =
        skillGuide(Skill.RUNECRAFTING.id) {
            layout(hideComponents = intArrayOf(8825, 8813))
            labels(8846 to "Runecrafting")
            pages { _, _ ->
                page(
                    names = arrayOf("Small pouch", "Nature rune", "Medium pouch", "Large pouch", "Blood rune", "Giant pouch", "Cosmic rune", "Skillcape$PREMIUM"),
                    levels = arrayOf("1", "1", "20", "40", "50", "60", "75", "99"),
                    items = intArrayOf(5509, 561, 5510, 5512, 565, 5514, 564, 9765),
                )
            }
        }

    private fun fishing() =
        skillGuide(Skill.FISHING.id) {
            layout(hideComponents = intArrayOf(8825, 8813))
            labels(8846 to "Fishing")
            pages { _, _ ->
                page(
                    names = arrayOf("Shrimps", "Trout", "Salmon", "Lobster", "Swordfish", "Monkfish$PREMIUM", "Dragon harpoon", "Shark", "Sea Turtle$PREMIUM", "Manta Ray$PREMIUM", ""),
                    levels = arrayOf("1", "20", "30", "40", "50", "60", "61", "70", "85", "95"),
                    items = intArrayOf(317, 335, 331, 377, 371, 7944, 21028, 383, 395, 389),
                )
            }
        }

    private fun cooking() =
        skillGuide(Skill.COOKING.id) {
            layout(hideComponents = intArrayOf(8825, 8813))
            labels(8846 to "Cooking")
            pages { _, _ ->
                page(
                    names = arrayOf("Shrimps", "Chicken", "Meat", "Bread", "Thin snail", "Trout", "Salmon", "Lobster", "Swordfish", "Monkfish$PREMIUM", "Shark", "Sea Turtle$PREMIUM", "Manta Ray$PREMIUM"),
                    levels = arrayOf("1", "1", "1", "10", "15", "20", "30", "40", "50", "60", "70", "85", "95"),
                    items = intArrayOf(315, 2140, 2142, 2309, 3369, 333, 329, 379, 373, 7946, 385, 397, 391),
                )
            }
        }

    private fun crafting() =
        skillGuide(Skill.CRAFTING.id) {
            layout(
                showComponents = intArrayOf(8827, 8828, 8838),
                hideComponents = intArrayOf(8841, 8850),
            )
            labels(8846 to "Spinning", 8823 to "Armor", 8824 to "Jewelry", 8827 to "Glass", 8837 to "Weaponry", 8840 to "Other")
            pages { _, child ->
                when (child) {
                    0 -> page(arrayOf("Ball of wool", "Bow string", "2 tick stringing", "1 tick stringing"), arrayOf("1", "10", "40", "70"), intArrayOf(1759, 1777))
                    1 -> page(
                        names = arrayOf("Leather gloves", "Leather boots", "Leather cowl", "Leather vambraces", "Leather body", "Leather chaps", "Coif", "Green d'hide vamb", "Green d'hide chaps", "Green d'hide body", "Blue d'hide vamb", "Blue d'hide chaps", "Blue d'hide body", "Slayer helmet", "Red d'hide vamb", "Red d'hide chaps", "Red d'hide body", "Black d'hide vamb", "Black d'hide chaps", "Black d'hide body"),
                        levels = arrayOf("1", "7", "9", "11", "14", "18", "39", "50", "54", "58", "62", "66", "70", "70", "73", "76", "79", "82", "85", "88"),
                        items = intArrayOf(1059, 1061, 1167, 1063, 1129, 1095, 1169, 1065, 1099, 1135, 2487, 2493, 2499, 11864, 2489, 2495, 2501, 2491, 2497, 2503),
                    )
                    2 -> page(
                        names = arrayOf("Gold ring", "Gold necklace", "Gold bracelet", "Gold amulet", "Cut sapphire", "Sapphire ring", "Sapphire necklace", "Sapphire bracelet", "Sapphire amulet", "Cut emerald", "Emerald ring", "Emerald necklace", "Emerald bracelet", "Emerald amulet", "Cut ruby", "Ruby ring", "Ruby necklace", "Ruby bracelet", "Cut diamond", "Diamond ring", "Ruby amulet", "Cut dragonstone", "Dragonstone ring", "Diamond necklace", "Diamond bracelet", "Cut onyx", "Onyx ring", "Diamond amulet", "Dragonstone necklace", "Dragonstone bracelet", "Dragonstone amulet", "Onyx necklace", "Onyx bracelet", "Onyx amulet"),
                        levels = arrayOf("5", "6", "7", "8", "20", "20", "22", "23", "24", "27", "27", "29", "30", "31", "34", "34", "40", "42", "43", "43", "50", "55", "55", "56", "58", "67", "67", "70", "72", "74", "80", "82", "84", "90"),
                        items = intArrayOf(1635, 1654, 11069, 1692, 1607, 1637, 1656, 11072, 1694, 1605, 1639, 1658, 11076, 1696, 1603, 1641, 1660, 11085, 1601, 1643, 1698, 1615, 1645, 1662, 11092, 6573, 6575, 1700, 1664, 11115, 1702, 6577, 11130, 6581),
                    )
                    3 -> page(arrayOf("Vial", "Empty cup", "Fishbowl", "Unpowered orb"), arrayOf("1", "18", "32", "48"), intArrayOf(229, 1980, 6667, 567))
                    4 -> page(arrayOf("Water battlestaff", "Earth battlestaff", "Fire battlestaff", "Air battlestaff"), arrayOf("51", "56", "61", "66"), intArrayOf(1395, 1399, 1393, 1397))
                    5 -> page(arrayOf("Crystal key", "Skillcape$PREMIUM"), arrayOf("60", "99"), intArrayOf(989, 9780))
                    else -> emptyPage()
                }
            }
        }

    private fun smithing() =
        skillGuide(Skill.SMITHING.id) {
            layout(showComponents = intArrayOf(8827, 8828, 8838, 8841, 8850))
            labels(8846 to "Smelting", 8823 to "Bronze", 8824 to "Iron", 8827 to "Steel", 8837 to "Mithril", 8840 to "Adamant", 8843 to "Runite", 8859 to "Special")
            pages { client, child ->
                when {
                    child == 0 -> page(
                        names = arrayOf(
                            "Bronze bar",
                            "Iron bar (${50 + ((client.getLevel(Skill.SMITHING) + 1) / 4)}% success)",
                            "Steel bar (2 coal & 1 iron ore)",
                            "Gold bar",
                            "Mithril bar (3 coal & 1 mithril ore)",
                            "Adamantite bar (4 coal & 1 adamantite ore)",
                            "Runite bar (6 coal & 1 runite ore)",
                        ),
                        levels = arrayOf("1", "15", "30", "40", "55", "70", "85"),
                        items = intArrayOf(2349, 2351, 2353, 2357, 2359, 2361, 2363),
                    )
                    child > 0 && child <= SmithingFrameDefinitions.smithingFrame.size -> {
                        val frame = SmithingFrameDefinitions.smithingFrame[child - 1]
                        SkillGuidePage(
                            frame.map { definition ->
                                SkillGuideEntry(
                                    text = client.GetItemName(definition.itemId),
                                    levelText = definition.levelRequired.toString(),
                                    itemId = definition.itemId,
                                    itemAmount = definition.outputAmount,
                                )
                            },
                        )
                    }
                    child == SmithingFrameDefinitions.smithingFrame.size + 1 -> page(arrayOf("Rockshell armour", "Dragonfire shield", "Skillcape$PREMIUM"), arrayOf("60", "90", "99"), intArrayOf(6129, 11284, 9795))
                    else -> emptyPage()
                }
            }
        }

    private fun agility() =
        skillGuide(Skill.AGILITY.id) {
            layout(hideComponents = intArrayOf(8825, 8813))
            labels(8846 to "Agility")
            pages { _, _ ->
                page(
                    names = arrayOf("Gnome Course", "Barbarian Course", "Yanille castle wall", "Crystal Shield", "Werewolf Course", "Taverly dungeon shortcut", "Wilderness course", "Shilo stepping stones ", "Prime boss shortcut", "Skillcape$PREMIUM"),
                    levels = arrayOf("1", "40", "50", "60", "60", "70", "70", "75", "85", "99"),
                    items = intArrayOf(751, 1365, -1, 4224, 4179, 4155, 964, -1, 4155, 9771),
                )
            }
        }

    private fun woodcutting() =
        skillGuide(Skill.WOODCUTTING.id) {
            layout(hideComponents = intArrayOf(8825))
            labels(8846 to "Axes", 8823 to "Logs", 8824 to "Misc")
            pages { _, child ->
                when (child) {
                    0 -> page(arrayOf("Bronze Axe", "Iron Axe", "Steel Axe", "Black Axe", "Mithril Axe", "Adamant Axe", "Rune Axe", "Dragon Axe"), arrayOf("1", "1", "6", "11", "21", "31", "41", "61"), intArrayOf(1351, 1349, 1353, 1361, 1355, 1357, 1359, 6739))
                    1 -> page(arrayOf("Logs", "Oak logs", "Willow logs", "Maple logs", "Yew logs", "Magic logs"), arrayOf("1", "15", "30", "45", "60", "75"), intArrayOf(1511, 1521, 1519, 1517, 1515, 1513))
                    2 -> page(arrayOf("Skillcape$PREMIUM"), arrayOf("99"), intArrayOf(9807))
                    else -> emptyPage()
                }
            }
        }

    private fun mining() =
        skillGuide(Skill.MINING.id) {
            layout(hideComponents = intArrayOf(8825))
            labels(8846 to "Pickaxes", 8823 to "Ores", 8824 to "Misc")
            pages { _, child ->
                when (child) {
                    0 -> page(arrayOf("Bronze Pickaxe", "Iron Pickaxe", "Steel Pickaxe", "Black Pickaxe", "Mithril Pickaxe", "Adamant Pickaxe", "Rune Pickaxe", "Dragon Pickaxe"), arrayOf("1", "1", "6", "11", "21", "31", "41", "61"), intArrayOf(1265, 1267, 1269, 12297, 1273, 1271, 1275, 11920))
                    1 -> page(arrayOf("Rune essence", "Copper ore", "Tin ore", "Iron ore", "Coal", "Gold ore", "Mithril ore", "Adamant ore", "Runite ore"), arrayOf("1", "1", "1", "15", "30", "40", "55", "70", "85"), intArrayOf(1436, 436, 438, 440, 453, 444, 447, 449, 451))
                    2 -> page(arrayOf("Skillcape$PREMIUM"), arrayOf("99"), intArrayOf(9792))
                    else -> emptyPage()
                }
            }
        }

    private fun slayer() =
        skillGuide(Skill.SLAYER.id) {
            layout(hideComponents = intArrayOf(8825))
            labels(8846 to "Master", 8823 to "Monsters", 8824 to "Misc")
            pages { _, child ->
                when (child) {
                    0 -> page(arrayOf("Mazchna (level 3 combat)", "Vannaka (level 3 combat)", "Duradel (level 50 combat)"), arrayOf("1", "50", "50"), intArrayOf(4155))
                    1 -> page(
                        names = arrayOf("Crawling hands", "Pyrefiend", "Albino bat", "Death spawn", "Jelly", "Head mourner", "Jungle horrors", "Skeletal hellhound", "Lesser demon", "Bloodvelds", "Greater demon", "Black demon", "Gargoyles", "Cave horrors", "Berserker Spirit", "Aberrant Spectres", "Tzhaar", "Mithril Dragon", "Abyssal demon", "Dagannoth Prime"),
                        levels = arrayOf("1", "20", "25", "30", "30", "45", "45", "50", "50", "53", "55", "60", "63", "65", "70", "73", "80", "83", "85", "86"),
                        items = intArrayOf(4133, 4138, -1, -1, 4142, -1, -1, -1, -1, 4141, -1, -1, 4147, 8900, -1, 4144, -1, -1, 4149, -1),
                    )
                    2 -> page(arrayOf("Skillcape$PREMIUM"), arrayOf("99"), intArrayOf(9786))
                    else -> emptyPage()
                }
            }
        }

    private fun firemaking() =
        skillGuide(Skill.FIREMAKING.id) {
            layout(hideComponents = intArrayOf(8825, 8813))
            labels(8846 to "Firemaking")
            pages { _, _ ->
                page(arrayOf("Logs", "Oak logs", "Willow logs", "Maple logs", "Yew logs", "Magic logs", "Skillcape$PREMIUM"), arrayOf("1", "15", "30", "45", "60", "75", "99"), intArrayOf(1511, 1521, 1519, 1517, 1515, 1513, 9804))
            }
        }

    private fun herblore() =
        skillGuide(Skill.HERBLORE.id) {
            layout(hideComponents = intArrayOf(8825))
            labels(8846 to "Potions", 8823 to "Herbs", 8824 to "Misc")
            pages { client, child ->
                when (child) {
                    0 -> page(
                        names = arrayOf(
                            "Attack Potion \\nGuam leaf & eye of newt",
                            "Strength Potion\\nTarromin & limpwurt root",
                            "Defence Potion\\nRanarr weed & white berries",
                            "Prayer potion\\nRanarr weed & snape grass",
                            "Super Attack Potion $PREMIUM\\nIrit leaf & eye of newt",
                            "Super Strength Potion$PREMIUM\\nKwuarm & limpwurt root",
                            "Super Restore Potion$PREMIUM\\nSnapdragon & red spiders' eggs",
                            "Super Defence Potion$PREMIUM\\nCadantine & white berries",
                            "Ranging Potion$PREMIUM\\nDwarf weed & wine of Zamorak",
                            "Antifire potion$PREMIUM\\nTorstol & willow root",
                            "Super Combat Potion$PREMIUM\\nTorstol, Super attack, strength & defence potion",
                            "Overload Potion$PREMIUM\\nCoconut, Ranging & Super combat potion",
                        ),
                        levels = arrayOf("3", "14", "25", "38", "46", "55", "60", "65", "75", "79", "88", "93"),
                        items = intArrayOf(121, 115, 133, 139, 145, 157, 3026, 163, 169, 2454, 12695, 11730),
                    )
                    1 -> SkillGuidePage(
                        HerbloreDefinitions.herbDefinitions.map { definition ->
                            SkillGuideEntry(
                                text = client.GetItemName(definition.cleanId) + if (definition.premiumOnly) " $PREMIUM" else "",
                                levelText = definition.requiredLevel.toString(),
                                itemId = definition.grimyId,
                            )
                        },
                    )
                    2 -> page(arrayOf("Skillcape$PREMIUM"), arrayOf("99"), intArrayOf(9774))
                    else -> emptyPage()
                }
            }
        }

    private fun fletching() =
        skillGuide(Skill.FLETCHING.id) {
            layout(hideComponents = intArrayOf(8825, 8813))
            labels(8846 to "Fletching")
            pages { _, _ ->
                page(
                    names = arrayOf("Arrow Shafts", "Oak Shortbow", "Oak Longbow", "Willow Shortbow", "Willow Longbow", "Maple Shortbow", "Maple Longbow", "Yew Shortbow", "Yew Longbow", "Magic Shortbow", "Magic Longbow"),
                    levels = arrayOf("1", "20", "25", "35", "40", "50", "55", "65", "70", "80", "85"),
                    items = intArrayOf(
                        52,
                        FletchingDefinitions.bowLogs[0].unstrungShortbowId,
                        FletchingDefinitions.bowLogs[0].unstrungLongbowId,
                        FletchingDefinitions.bowLogs[1].unstrungShortbowId,
                        FletchingDefinitions.bowLogs[1].unstrungLongbowId,
                        FletchingDefinitions.bowLogs[2].unstrungShortbowId,
                        FletchingDefinitions.bowLogs[2].unstrungLongbowId,
                        FletchingDefinitions.bowLogs[3].unstrungShortbowId,
                        FletchingDefinitions.bowLogs[3].unstrungLongbowId,
                        FletchingDefinitions.bowLogs[4].unstrungShortbowId,
                        FletchingDefinitions.bowLogs[4].unstrungLongbowId,
                    ),
                )
            }
        }

    private fun farming() =
        skillGuide(Skill.FARMING.id) {
            layout(
                showComponents = intArrayOf(8827, 8828, 8838, 8841),
                hideComponents = intArrayOf(8850),
            )
            labels(8846 to "Allotment", 8823 to "Flower", 8824 to "Bush", 8827 to "Herb", 8837 to "Tree", 8840 to "Fruit tree", 8843 to "Misc")
            pages { _, child ->
                when (child) {
                    0 -> page(
                        names = arrayOf(
                            "Potatoes \\nPayment: 3 compost", "Onions \\nPayment: 1 sack of potato", "Cabbage \\nPayment: 1 sack of onion", "Tomatoes \\nPayment: 1 sack of cabbage",
                            "Sweetcorn \\nPayment: 2 basket of tomato", "Strawberries $PREMIUM\\nPayment: 12 sweetcorn", "Watermelons $PREMIUM\\nPayment: 3 basket of strawberries", "Snape grass $PREMIUM\\nPayment: 15 watermelon",
                        ),
                        levels = arrayOf("1", "5", "9", "16", "25", "31", "47", "59"),
                        items = intArrayOf(1942, 1957, 1965, 1982, 5986, 5504, 5982, 231),
                    )
                    1 -> page(
                        names = arrayOf("Marigold \\nPayment: 2 compost", "Rosemary \\nPayment: 2 Marigold", "Nasturtium \\nPayment: 2 Rosemary", "Woad \\nPayment: 2 Nasturtium", "Limpwurt plants \\nPayment: 2 Woad leaf"),
                        levels = arrayOf("3", "13", "22", "26", "34"),
                        items = intArrayOf(6010, 6014, 6012, 1793, 225),
                    )
                    2 -> page(
                        names = arrayOf("Redberry bushes $PREMIUM\\nPayment: 5 super compost", "Dwellberry bushes $PREMIUM\\nPayment: 8 redberries", "Jangerberry bushes $PREMIUM\\nPayment: 8 dwellberries", "White berry bushes $PREMIUM\\nPayment: 8 jangerberries", "Grape bushes$PREMIUM\\nPayment: 8 jangerberries & 8 redberries"),
                        levels = arrayOf("22", "36", "48", "59", "68"),
                        items = intArrayOf(1951, 2126, 247, 239, 1987),
                    )
                    3 -> page(
                        names = arrayOf("Guam", "Marrentill", "Tarromin", "Ranarr", "Irit $PREMIUM", "Kwuarm $PREMIUM", "Snapdragon $PREMIUM", "Cadantine $PREMIUM", "Dwarf weed $PREMIUM", "Torstol $PREMIUM"),
                        levels = arrayOf("8", "12", "19", "34", "44", "56", "63", "70", "79", "85"),
                        items = intArrayOf(199, 201, 203, 207, 209, 213, 3051, 215, 217, 219),
                    )
                    4 -> page(
                        names = arrayOf("Acorn trees \\nPayment: 1 basket of Tomatoes & 1 basket of Strawberries", "Willow trees $PREMIUM\\nPayment: 3 oak roots", "Maple trees $PREMIUM\\nPayment: 3 willow roots", "Yew trees $PREMIUM\\nPayment: 3 maple roots", "Magic trees $PREMIUM\\nPayment: 3 yew roots"),
                        levels = arrayOf("15", "30", "45", "60", "75"),
                        items = intArrayOf(1521, 1519, 1517, 1515, 1513),
                    )
                    5 -> page(
                        names = arrayOf("Apple trees \\nPayment: 9 sweetcorn", "Banana trees \\nPayment: 2 basket of apples", "Orange trees \\nPayment: 2 basket of bananas", "Curry trees \\nPayment: 2 basket of oranges", "Pineapple plants $PREMIUM\\nPayment: 10 curry", "Papaya trees $PREMIUM\\nPayment: 10 pineapple", "Palm trees $PREMIUM\\nPayment: 10 papaya"),
                        levels = arrayOf("27", "33", "39", "45", "51", "57", "63"),
                        items = intArrayOf(1955, 1963, 2108, 5970, 2114, 5972, 5974),
                    )
                    6 -> page(arrayOf("Skillcape$PREMIUM"), arrayOf("99"), intArrayOf(9810))
                    else -> emptyPage()
                }
            }
        }

    private fun page(
        names: Array<String>,
        levels: Array<String> = emptyArray(),
        items: IntArray = intArrayOf(),
        amounts: IntArray? = null,
    ): SkillGuidePage =
        SkillGuidePage(
            names.indices.map { index ->
                SkillGuideEntry(
                    text = names[index],
                    levelText = levels.getOrNull(index),
                    itemId = items.getOrElse(index) { -1 },
                    itemAmount = amounts?.getOrNull(index),
                )
            },
        )

    private fun emptyPage(): SkillGuidePage = SkillGuidePage()
}
