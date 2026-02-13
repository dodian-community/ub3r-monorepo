package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.Constants
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.Skills
import net.dodian.uber.game.model.player.skills.agility.Werewolf
import net.dodian.uber.game.netty.listener.out.SendMessage
import java.util.Date
import java.util.Objects

internal object LegacyFirstClickNpcContent {
    val npcIds: IntArray = intArrayOf(
        2794, // Sheep
        5792, 3306,
        5927, 683, 2053, 3951, 376, 8051, 659, 3640, 556, 557, 4808, 3541, 520, 5842, 943,
    )

    fun onFirstClick(client: Client, npc: Npc): Boolean {
        return when (npc.id) {
            2794 -> {
                if (client.playerHasItem(1735)) {
                    client.addItem(1737, 1)
                    client.checkItemUpdate()
                } else {
                    client.send(SendMessage("You need some shears to shear this sheep!"))
                }
                true
            }

            5792 -> {
                client.triggerTele(3045, 3372, 0, false)
                client.send(SendMessage("Welcome to the party room!"))
                true
            }

            3306 -> {
                var peopleInEdge = 0
                var peopleInWild = 0
                for (index in 0 until Constants.maxPlayers) {
                    val checkPlayer = PlayerHandler.players[index] as? Client ?: continue
                    if (checkPlayer.inWildy()) {
                        peopleInWild++
                    } else if (checkPlayer.inEdgeville()) {
                        peopleInEdge++
                    }
                }
                client.showNPCChat(
                    3306,
                    590,
                    arrayOf(
                        "There is currently $peopleInWild player${if (peopleInWild != 1) "s" else ""} in the wild!",
                        "There is $peopleInEdge player${if (peopleInEdge != 1) "s" else ""} in Edgeville!",
                    ),
                )
                true
            }

            5927 -> {
                Werewolf(client).handStick()
                true
            }

            683 -> {
                client.WanneShop = 11
                true
            }

            2053 -> {
                client.WanneShop = 32
                true
            }

            3951 -> {
                if (client.premium) {
                    client.ReplaceObject(2728, 3349, 2391, 0, 0)
                    client.ReplaceObject(2729, 3349, 2392, -2, 0)
                    client.showNPCChat(npc.id, 590, arrayOf("Welcome to the Guild of Legends", "Enjoy your stay."))
                } else {
                    client.showNPCChat(npc.id, 595, arrayOf("You must be a premium member to enter", "Visit Dodian.net to subscribe"))
                }
                true
            }

            376 -> {
                if (client.playerRights == 2) {
                    client.triggerTele(2772, 3234, 0, false)
                }
                true
            }

            8051 -> {
                client.NpcWanneTalk = 8051
                true
            }

            659 -> {
                client.NpcWanneTalk = 1000
                client.convoId = 1001
                true
            }

            3640 -> {
                client.WanneShop = 17
                true
            }

            556 -> {
                client.WanneShop = 31
                true
            }

            557 -> {
                npc.requestAnim(5643, 0)
                for (skill in listOf(0, 1, 2, 4)) {
                    val skillType = Objects.requireNonNull(Skill.getSkill(skill))
                    val maxLevel = Skills.getLevelForExperience(client.getExperience(skillType))
                    client.boost(5 + (maxLevel * 0.15).toInt(), skillType)
                }
                val ticks = (1 + Skills.getLevelForExperience(client.getExperience(Skill.HERBLORE))) * 2
                client.addEffectTime(2, 200 + ticks)
                client.send(SendMessage("The monk boost your stats!"))
                true
            }

            4808 -> {
                client.WanneShop = 34
                true
            }

            3541 -> {
                client.WanneShop = 35
                true
            }

            520 -> {
                client.NpcWanneTalk = 19
                client.convoId = 4
                true
            }

            5842 -> {
                val canClaim = Date().before(Date("06/1/2024")) && !client.checkItem(7927)
                if (canClaim) {
                    client.showNPCChat(npc.id, 595, arrayOf("Here take a easter ring for all your troubles.", "Enjoy your stay at Dodian."))
                    client.addItem(7927, 1)
                    client.checkItemUpdate()
                } else {
                    client.showNPCChat(npc.id, 595, arrayOf(if (client.checkItem(7927)) "You already got the ring." else "It is not May anymore."))
                }
                true
            }

            943 -> {
                var count = 0
                for (player in PlayerHandler.players) {
                    if (player != null && player.wildyLevel > 0) {
                        count++
                    }
                }
                npc.setText("There are currently $count people in the wilderness")
                true
            }

            else -> false
        }
    }
}
