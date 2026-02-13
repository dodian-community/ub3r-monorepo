package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.thieving.Thieving

internal object LegacySecondClickNpcContent {
    val npcIds: IntArray = intArrayOf(
        3086, 3257,
        5034, 844, 462,
        1779,
        506, 527, 4965, 1032, 538, 6478, 3890, 535, 6060, 1027, 5809, 6059, 4642,
    )

    fun onFirstClick(client: Client, npc: Npc): Boolean {
        return when (npc.id) {
            1779 -> {
                client.showNPCChat(1779, 605, arrayOf("What are you even doing in here?!", "Begone from me!"))
                true
            }
            5809 -> {
                client.openTan()
                true
            }
            else -> false
        }
    }

    fun onSecondClick(client: Client, npc: Npc): Boolean {
        when (npc.id) {
            3086, 3257 -> {
                Thieving.attemptSteal(client, npc.id, npc.position)
                return true
            }
            5034, 844, 462 -> {
                client.stairs = 26
                client.stairDistance = 1
                return true
            }
            1779 -> {
                client.showNPCChat(1779, 605, arrayOf("What are you even doing in here?!", "Begone from me!"))
                return true
            }
            506, 527 -> client.WanneShop = 3
            4965 -> client.WanneShop = 4
            1032 -> client.WanneShop = 5
            538 -> client.WanneShop = 6
            6478 -> client.WanneShop = 7
            3890 -> client.WanneShop = 8
            535 -> client.WanneShop = 10
            6060 -> client.WanneShop = 11
            1027 -> client.WanneShop = 16
            5809 -> client.WanneShop = 18
            6059 -> client.WanneShop = 30
            4642 -> client.WanneShop = 36
            else -> return false
        }
        return true
    }
}
