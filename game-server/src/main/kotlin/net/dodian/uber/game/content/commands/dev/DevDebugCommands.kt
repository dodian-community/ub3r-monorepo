package net.dodian.uber.game.content.commands.dev

import net.dodian.uber.game.Server
import net.dodian.uber.game.content.commands.CommandContent
import net.dodian.uber.game.content.commands.CommandContext
import net.dodian.uber.game.content.commands.commands
import net.dodian.uber.game.event.GameEventScheduler
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.Skills
import net.dodian.uber.game.systems.api.content.ContentCoroutines.gameClock
import net.dodian.uber.game.systems.api.content.ContentCoroutines.npcTaskCoroutine
import net.dodian.uber.game.systems.api.content.ContentCoroutines.playerTaskCoroutine
import net.dodian.uber.game.systems.api.content.ContentCoroutines.worldTaskCoroutine
import net.dodian.uber.game.content.skills.core.progression.SkillProgressionService
import java.util.function.BooleanSupplier
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.party.Balloons
import net.dodian.utilities.Misc

object DevDebugCommands : CommandContent {
    override fun definitions() =
        commands {
            command(
                "npca", "loot", "death", "damage", "dharok", "heal", "farm", "immune", "rehp",
                "split", "party", "event", "gem", "rune", "boost_on", "boost_off",
                "wcor", "pcor", "ncor",
            ) {
                handleDevDebug(this)
            }
        }
}

private fun handleDevDebug(context: CommandContext): Boolean {
    val client = context.client
    val cmd = context.parts
    if (!context.specialRights) {
        return false
    }
    when (context.alias) {
        "npca" -> {
            return try {
                val id = cmd[1].toInt()
                val data = Server.npcManager.getData(id)
                if (data == null) {
                    context.reply("No npc definition found for id $id.")
                    return true
                }
                data.attackEmote = cmd[2].toInt()
                true
            } catch (_: Exception) {
                context.usage("Wrong usage.. ::${cmd[0]} npcid animationId")
            }
        }
        "loot" -> {
            client.instaLoot = !client.instaLoot
            context.reply("You turned insta loot ${if (client.instaLoot) "on" else "off"}!")
        }
        "death" -> {
            val type = cmd[1].toInt()
            if (type > 0 && type <= Entity.hitType.values().size) {
                client.dealDamage(null, client.currentHealth, Entity.hitType.values()[type - 1])
            } else {
                context.reply("Only type 1 to ${Entity.hitType.values().size} works!")
            }
        }
        "damage" -> client.dealDamage(null, 20, Entity.hitType.CRIT)
        "dharok" -> client.dealDamage(null, client.currentHealth - 1, Entity.hitType.CRIT)
        "heal" -> {
            val overHeal = (client.maxHealth * 0.15).toInt()
            client.heal(client.maxHealth + overHeal, overHeal)
        }
        "farm" -> {
            return try {
                var gotValue = true
                val value = cmd[1].toInt()
                val config = 0
                when (value) {
                    0 -> {
                        for (i in 0..3) client.varbit(4771 + i, config)
                        client.varbit(4775, config)
                        client.varbit(4776, config)
                    }
                    1 -> {
                        val startConfig = intArrayOf(5, 5, 7, 5)
                        val stage = intArrayOf(1, 2, 2, 1)
                        val patch = intArrayOf(1, 2, 3, 0)
                        for (i in startConfig.indices) {
                            val check = patch[i] shl 6
                            client.varbit(4771 + i, (startConfig[i] + stage[i]) or check)
                        }
                    }
                    2 -> client.varbit(4771, 4 + 246)
                    3 -> {
                        val values = intArrayOf(3, 10, 17, 24, 31, 38, 45, 52, 67, 74, 81, 88, 95, 102)
                        val configSlot = values.size - 1
                        val stageValue = 2
                        val addExtra = if (configSlot > 7) 8 else 0
                        val disease = 1
                        val test = values[configSlot] + stageValue or (disease shl 6)
                        client.varbit(4774, if (disease == 1) 168 + stageValue else if (disease == 2) test - (5 + configSlot * 4 + addExtra) else test)
                    }
                    4 -> {
                        client.cancelFarmDebugTask()
                        val farmConfig = intArrayOf(0)
                        client.farmDebugTaskHandle = GameEventScheduler.schedule(
                            1,
                            1,
                            BooleanSupplier {
                                if (client.disconnected || !client.isActive) {
                                    client.farmDebugTaskHandle = null
                                    false
                                } else if (farmConfig[0] >= 2000) {
                                    context.reply("Finished farming config test.")
                                    client.farmDebugTaskHandle = null
                                    false
                                } else {
                                    context.reply("config = ${farmConfig[0]}")
                                    client.varbit(4771, farmConfig[0])
                                    farmConfig[0]++
                                    true
                                }
                            },
                        )
                    }
                    else -> gotValue = false
                }
                context.reply(if (gotValue) "You set farming config to $value" else "Could not find a value!")
                true
            } catch (_: Exception) {
                context.usage("Wrong usage.. ::${cmd[0]} patchId")
            }
        }
        "immune" -> {
            client.immune = !client.immune
            context.reply("You set immune as ${client.immune}")
        }
        "rehp" -> {
            client.reloadHp = !client.reloadHp
            context.reply("You set reload hp as ${client.reloadHp}")
        }
        "split" -> {
            val chance = cmd[1].toInt()
            val array = doubleArrayOf(0.14, 0.4, 0.3, 0.08, 0.08)
            val parts = arrayOf("helm", "body", "legs", "feet", "boots")
            for (i in array.indices) {
                context.reply("$chance is started and of that ${parts[i]} should be ${(chance * array[i]).toInt()} stats!")
            }
        }
        "party" -> Balloons.triggerPartyEvent(client)
        "event" -> Balloons.triggerBalloonEvent(client)
        "gem" -> {
            val chance = doubleArrayOf(23.4, 11.7, 7.03, 3.91, 3.91, 3.12)
            val gemId = intArrayOf(1625, 1627, 1629, 1623, 1621, 1619, 1617)
            var rolledChance = 0
            var gem = -1
            val roll = Misc.chance(10000)
            for (i in chance.indices) {
                if (gem != -1) {
                    break
                }
                rolledChance += (chance[i] * 100).toInt()
                gem =
                    if (roll <= rolledChance) {
                        gemId[i + 1]
                    } else if (i + 1 == chance.size) {
                        gemId[0]
                    } else {
                        -1
                    }
            }
            context.reply("You found gem..${client.GetItemName(gem)}($gem)")
        }
        "rune" -> {
            val level = cmd[1].toInt()
            var deleteOne = 0
            var deleteTwo = 0
            var deleteThree = 0
            var deleteFour = 0
            val value = doubleArrayOf(1.25, 2.5, 5.0)
            repeat(100) {
                if (Math.random() * 100 < 100.0 - (level / value[0])) deleteFour++
                else if (Math.random() * 100 < 100.0 - (level / value[1])) deleteThree++
                else if (Math.random() * 100 < 100.0 - (level / value[2])) deleteTwo++
                else deleteOne++
            }
            context.reply("At level $level Runecraft, Four=$deleteFour, Three=$deleteThree, Two=$deleteTwo, One=$deleteOne.")
        }
        "boost_on" -> {
            client.boost(1337, Skill.STRENGTH)
            client.boost(1337, Skill.DEFENCE)
            client.boost(1337, Skill.ATTACK)
            client.boost(1337, Skill.RANGED)
            client.boost(1337, Skill.MAGIC)
        }
        "boost_off" -> {
            for (i in 0 until 7) {
                if (i != 3 && i != 5) {
                    client.boostedLevel[i] = 0
                    Skill.getSkill(i)?.let { SkillProgressionService.refresh(client, it) }
                }
            }
        }
        "wcor" -> {
            context.reply("Scheduled world coroutine demo.")
            worldTaskCoroutine {
                client.send(SendMessage("[${gameClock()}] world: start"))
                delay(3)
                client.send(SendMessage("[${gameClock()}] world: step-2"))
                delay(7)
                client.send(SendMessage("[${gameClock()}] world: stop"))
                stop()
            }
        }
        "pcor" -> {
            context.reply("Scheduled player coroutine demo.")
            playerTaskCoroutine(client) {
                player.send(SendMessage("[${gameClock()}] player: start"))
                delay(2)
                player.send(SendMessage("[${gameClock()}] player: step-2"))
                delay(2)
                player.send(SendMessage("[${gameClock()}] player: stop"))
                stop()
            }
        }
        "ncor" -> {
            val targetNpc =
                Server.npcManager
                    .getNpcs()
                    .firstOrNull { npc ->
                        npc.position.withinDistance(client.position, 8)
                    }
            if (targetNpc == null) {
                context.reply("No nearby npc in range 8 for npc coroutine demo.")
                return true
            }
            context.reply("Scheduled npc coroutine demo on npc ${targetNpc.id} (${targetNpc.position}).")
            npcTaskCoroutine(targetNpc) {
                npc.setText("[${gameClock()}] npc coroutine start")
                delay(2)
                npc.setText("[${gameClock()}] npc coroutine step-2")
                delay(2)
                npc.setText("[${gameClock()}] npc coroutine stop")
                stop()
            }
        }
        else -> return false
    }
    return true
}
