package net.dodian.uber.game.ui

import net.dodian.uber.game.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.ui.buttons.buttonBinding
import net.dodian.uber.game.netty.listener.out.ShowInterface
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendScrollbar
import net.dodian.uber.game.netty.listener.out.SendString
import net.dodian.uber.game.netty.listener.out.SendItemOnInterface
import net.dodian.uber.game.model.entity.player.Client
import java.util.concurrent.ConcurrentHashMap

object TeleportInterface : InterfaceButtonContent {
    private const val INTERFACE_ID = 58000

    data class TeleportData(
        val name: String,
        val x: Int,
        val y: Int,
        val z: Int = 0,
        val category: Int, // 1 = Minigames, 2 = Skilling, 3 = Monsters, 4 = PK, 5 = Bosses
        val desc1: String,
        val desc2: String,
        val displayItems: IntArray = intArrayOf(-1, -1, -1)
    )

    private val teleports = listOf(
        // Minigames
        TeleportData("Barrows", 3565, 3315, 0, 1, "43 Prayer is highly recommended", "Don't forget your spade!", intArrayOf(4718, 7462, 4734)),
        TeleportData("Duel Arena", 3365, 3265, 0, 1, "Stake at your own risk!", "", intArrayOf(4151, -1, 5698)),
        TeleportData("Fight Caves", 2438, 5168, 0, 1, "43 Prayer & High Range is recommended", "", intArrayOf(6529, 6570, 21295)),
        TeleportData("Pest Control", 2662, 2655, 0, 1, "Having a group is highly recommended", "", intArrayOf(11663, 11664, 11665)),
        TeleportData("Warrior Guild", 2846, 3541, 0, 1, "Get your defenders here!", "", intArrayOf(8844, 8850, 12954)),
        
        // Skilling
        TeleportData("Gnome Agility", 2480, 3437, 0, 2, "Train Agility here!", "", intArrayOf(9773, 9771, 9772)),
        TeleportData("Barbarian Agility", 2546, 3551, 0, 2, "Train Agility here!", "", intArrayOf(9773, 9771, 9772)),
        TeleportData("Mining Ess", 3039, 4836, 0, 2, "Train Mining & Runecrafting here!", "", intArrayOf(9794, 9792, 9793)),
        TeleportData("Fishing Guild", 2594, 3415, 0, 2, "Train Fishing here!", "", intArrayOf(9800, 9798, 9799)),
        TeleportData("Catherby Farming", 2809, 3435, 0, 2, "Train Farming here!", "", intArrayOf(9812, 9810, 9811)),
        TeleportData("Woodcutting Guild", 1587, 3488, 0, 2, "Train Woodcutting here!", "", intArrayOf(9809, 9807, 9808)),

        // Monsters
        TeleportData("Rock Crabs", 2676, 3711, 0, 3, "Medium level training", "", intArrayOf(-1, 411, -1)),
        TeleportData("Sand Crabs", 1726, 3463, 0, 3, "Higher level training", "", intArrayOf(-1, 411, -1)),
        TeleportData("Goblins", 3259, 3228, 0, 3, "Low level training", "", intArrayOf(-1, 288, -1)),
        TeleportData("Hill Giants", 3117, 9856, 0, 3, "Teleport directly to hill giants", "", intArrayOf()),
        TeleportData("Slayer Tower", 3429, 3538, 0, 3, "The home of many slayer monsters", "", intArrayOf(-1, 4151, -1)),
        TeleportData("Brimhaven Dungeon", 2681, 9563, 0, 3, "The home of many slayer monsters", "", intArrayOf()),
        TeleportData("Taverley Dungeon", 2884, 9800, 0, 3, "The home of many slayer monsters", "", intArrayOf()),

        // PK
        TeleportData("Mage Bank", 2540, 4717, 0, 4, "You will be teleported in a safe area", "", intArrayOf(2412, 2413, 2414)),
        TeleportData("Castle (14 Wild)", 3002, 3626, 0, 4, "This teleport is in 14 Wilderness", "", intArrayOf()),
        TeleportData("East Dragons", 3333, 3666, 0, 4, "This teleport is in 19 Wilderness", "", intArrayOf(-1, 1540, -1)),
        TeleportData("West Dragons", 2976, 3597, 0, 4, "This teleport is in 10 Wilderness", "", intArrayOf(-1, 1540, -1)),

        // Bosses
        TeleportData("King Black Dragon", 2997, 3849, 0, 5, "This teleport is in 40+ wilderness", "High combat recommended", intArrayOf(-1, 12653, -1)),
        TeleportData("Kalphite Queen", 3507, 9494, 0, 5, "High combat lvl recommended", "Multiple attack types recommended", intArrayOf(12654, 2513, 7981)),
        TeleportData("Zulrah", 2206, 3056, 0, 5, "High range & magic is highly recommended", "Beware of her poisonous venom!", intArrayOf(12921, 12939, 12940)),
        TeleportData("Kraken", 2276, 10000, 0, 5, "Make sure to have high magic defence", "Kraken is stronger than usual!", intArrayOf(12004, 12655, 12004)),
        TeleportData("Vorkath", 2276, 4036, 0, 5, "Vorkath, the Dragon King.", "Do you have what it takes?", intArrayOf(11286, 21992, 22006))
    )

    private val playerSelectedCategory = ConcurrentHashMap<String, Int>()
    private val playerSelectedTeleportIndex = ConcurrentHashMap<String, Int>()
    private val playerFavorites = ConcurrentHashMap<String, MutableSet<String>>()

    private fun getTeleportsForCategory(client: Client, category: Int): List<TeleportData> {
        return if (category == 0) {
            val favs = playerFavorites[client.playerName] ?: return emptyList()
            teleports.filter { favs.contains(it.name) }
        } else {
            teleports.filter { it.category == category }
        }
    }

    fun open(client: Client, category: Int, selectedIndex: Int) {
        playerSelectedCategory[client.playerName] = category
        playerSelectedTeleportIndex[client.playerName] = selectedIndex

        val list = getTeleportsForCategory(client, category)
        val size = list.size

        // 1. Send TITLES
        val titles = arrayOf("Favorites", "Minigames", "Skilling", "Monster Killing", "Player Killing", "Boss Killing")
        for (index in 0 until 6) {
            val color = if (category == index) "<col=ffffff>" else "<col=ff9933>"
            client.send(SendString(color + titles[index], 58009 + index * 4))
        }

        // 2. Send Category list items (58052, step 2)
        val maxDisplay = maxOf(size, 9)
        for (index in 0 until maxDisplay) {
            val stringId = 58052 + index * 2
            if (index >= size) {
                client.send(SendString("", stringId))
                continue
            }
            val tele = list[index]
            val prefix = if (selectedIndex == index) "<col=ffffff>" else ""
            val isFav = playerFavorites[client.playerName]?.contains(tele.name) ?: false
            val favoriteIcon = if (isFav) "<clan=6> " else ""
            client.send(SendString(favoriteIcon + prefix + tele.name, stringId))
        }

        // 3. Send Selected Teleport Details
        val selectedTele = if (selectedIndex in 0 until size) list[selectedIndex] else null
        display(client, selectedTele)

        // 4. Send Scrollbar
        client.send(SendScrollbar(58050, if (size <= 9) 225 else (size * 25)))

        // 5. Open Interface 58000
        client.openInterface(INTERFACE_ID)
    }

    fun display(client: Client, tele: TeleportData?) {
        val isFav = tele != null && (playerFavorites[client.playerName]?.contains(tele.name) ?: false)
        client.varbit(348, if (isFav) 0 else 1)

        if (tele == null) {
            client.send(SendString("", 58031))
            client.send(SendString("You do not have any teleport selected", 58032))
            client.send(SendString("", 58033))
            client.send(SendItemOnInterface(58041, intArrayOf(-1, -1, -1), intArrayOf(1, 1, 1)))
            return
        }

        client.send(SendString(tele.name, 58031))
        client.send(SendString("<col=ff7000>" + tele.desc1, 58032))
        client.send(SendString("<col=ff7000>" + tele.desc2, 58033))

        val displayItemIds = tele.displayItems
        val itemIds = IntArray(3) { -1 }
        val amounts = IntArray(3) { 1 }
        for (i in 0 until minOf(displayItemIds.size, 3)) {
            itemIds[i] = displayItemIds[i]
            if (displayItemIds[i] != -1) {
                if (displayItemIds[i] == 995 || displayItemIds[i] == 6529) {
                    amounts[i] = 50000
                }
            }
        }
        client.send(SendItemOnInterface(58041, itemIds, amounts))
    }

    fun favorite(client: Client) {
        val category = playerSelectedCategory[client.playerName] ?: 0
        val list = getTeleportsForCategory(client, category)
        val selectedIndex = playerSelectedTeleportIndex[client.playerName] ?: 0
        if (selectedIndex !in list.indices) {
            client.sendMessage("You have not selected a teleport to favorite!")
            client.varbit(348, 1)
            return
        }
        val tele = list[selectedIndex]
        val favs = playerFavorites.computeIfAbsent(client.playerName) { ConcurrentHashMap.newKeySet() }
        val isFav = favs.contains(tele.name)
        if (isFav) {
            favs.remove(tele.name)
            client.sendMessage("You have unfavorited the ${tele.name} teleport.")
        } else {
            favs.add(tele.name)
            client.sendMessage("You have favorited the ${tele.name} teleport.")
        }
        open(client, category, if (category == 0) 0 else selectedIndex)
    }

    fun teleport(client: Client) {
        val category = playerSelectedCategory[client.playerName] ?: 0
        val list = getTeleportsForCategory(client, category)
        val selectedIndex = playerSelectedTeleportIndex[client.playerName] ?: 0
        if (selectedIndex !in list.indices) {
            client.sendMessage("You have not selected a destination to teleport to!")
            return
        }
        val tele = list[selectedIndex]
        client.triggerTele(tele.x, tele.y, tele.z, false)
        client.sendMessage("You have teleported to ${tele.name}.")
        client.send(RemoveInterfaces())
    }

    override val bindings = buildList {
        
        // Category tabs
        add(buttonBinding(58000, 1, "teleport.tab.favorites", intArrayOf(58006)) { client, _ ->
            open(client, 0, 0)
            true
        })
        add(buttonBinding(58000, 2, "teleport.tab.minigames", intArrayOf(58010)) { client, _ ->
            open(client, 1, 0)
            true
        })
        add(buttonBinding(58000, 3, "teleport.tab.skilling", intArrayOf(58014)) { client, _ ->
            open(client, 2, 0)
            true
        })
        add(buttonBinding(58000, 4, "teleport.tab.monsters", intArrayOf(58018)) { client, _ ->
            open(client, 3, 0)
            true
        })
        add(buttonBinding(58000, 5, "teleport.tab.pk", intArrayOf(58022)) { client, _ ->
            open(client, 4, 0)
            true
        })
        add(buttonBinding(58000, 6, "teleport.tab.bosses", intArrayOf(58026)) { client, _ ->
            open(client, 5, 0)
            true
        })
        
        // Teleport action
        add(buttonBinding(58000, 7, "teleport.execute", intArrayOf(58035)) { client, _ ->
            teleport(client)
            true
        })
        
        // Favorite toggle
        add(buttonBinding(58000, 8, "teleport.favorite.toggle", intArrayOf(58039, 58040)) { client, _ ->
            favorite(client)
            true
        })

        // List item selection
        val listItemButtons = (58052..58120 step 2).toList().toIntArray()
        add(buttonBinding(58000, 9, "teleport.list.select", listItemButtons) { client, request ->
            val category = playerSelectedCategory[client.playerName] ?: 0
            val list = getTeleportsForCategory(client, category)
            val index = (request.rawButtonId - 58052) / 2
            if (index in list.indices) {
                open(client, category, index)
            }
            true
        })
    }
}