package net.dodian.uber.game.content.commands.dev

import net.dodian.uber.game.Server
import net.dodian.uber.game.content.commands.CommandContent
import net.dodian.uber.game.content.commands.CommandContext
import net.dodian.uber.game.content.commands.commandLogger
import net.dodian.uber.game.content.commands.commands
import net.dodian.uber.game.content.commands.numericToken
import net.dodian.uber.game.model.UpdateFlag
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.Skills
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendString
import net.dodian.uber.game.persistence.command.CommandDbService
import net.dodian.uber.game.content.skills.core.progression.SkillAdminService
import net.dodian.uber.game.content.skills.core.progression.SkillProgressionService

object DevAccountAndStateCommands : CommandContent {
    override fun definitions() =
        commands {
            command(
                "rank", "combat", "remitem", "remskill", "pouch", "update", "resettask",
                "reloaditems", "setlevel", "setxp", "reset", "master", "quest", "quest_reward",
                "moooo", "busy",
            ) {
                handleDevAccountState(this)
            }
        }
}

private fun handleDevAccountState(context: CommandContext): Boolean {
    val client = context.client
    val command = context.rawCommand
    val cmd = context.parts
    if (!context.specialRights && context.alias in setOf("rank", "combat", "remitem", "remskill", "pouch", "update", "resettask", "reloaditems", "setlevel", "setxp")) {
        return false
    }
    when {
        context.alias == "rank" && context.specialRights -> {
            return try {
                val rank = cmd[1]
                var rankId = -1
                val name = command.substring(cmd[0].length + cmd[1].length + 2)
                val other = PlayerHandler.getPlayer(name) as? Client
                when (rank) {
                    "normal" -> rankId = 40
                    "premium" -> rankId = 11
                    "mod" -> rankId = 5
                    "trial" -> rankId = 9
                }
                if (rankId == -1) {
                    context.reply("Only available ranks: 'normal', 'premium', 'trial', 'mod'")
                    return true
                }
                CommandDbService.submit(
                    "set-rank",
                    { CommandDbService.updateRank(name, rankId) },
                    { _ ->
                        if (!client.disconnected) {
                            other?.disconnected = true
                            context.reply("You set $name to a $rank!")
                        }
                    },
                    { exception ->
                        if (!client.disconnected) {
                            context.reply("Sql issue! Contact a admin!")
                            commandLogger.debug("set-rank failed for {}", name, exception)
                        }
                    },
                )
                true
            } catch (_: Exception) {
                context.usage("Wrong usage.. ::${cmd[0]} rank playername")
            }
        }
        context.alias == "combat" && context.specialRights -> {
            return try {
                val level = cmd[1].toInt()
                client.customCombat = if (level > 255 || level < 0) -1 else level
                context.reply(if (client.customCombat == -1) "You reset back to normal combat!" else "You set your own combat to level: $level")
                client.updateFlags.setRequired(UpdateFlag.APPEARANCE, true)
                true
            } catch (_: Exception) {
                context.usage("Wrong usage.. ::${cmd[0]} combat")
            }
        }
        context.alias == "remitem" && context.specialRights -> {
            return try {
                val id = cmd[1].toInt()
                val amount = cmd[2].toInt()
                val user = command.substring(cmd[0].length + cmd[1].length + cmd[2].length + 3)
                client.removeItemsFromPlayer(user, id, amount)
                true
            } catch (_: Exception) {
                context.usage("Wrong usage.. ::${cmd[0]} id amount playername")
            }
        }
        context.alias == "remskill" && context.specialRights -> {
            return try {
                val id = if (numericToken.matches(cmd[1])) cmd[1].toInt() else client.getSkillId(cmd[1])
                if (id < 0 || id > 20) {
                    context.reply("Skills are between 0 - 20!")
                    return true
                }
                val xp = if (cmd[2].toInt() < 1) 0 else cmd[2].toInt()
                val user = command.substring(cmd[0].length + cmd[1].length + cmd[2].length + 3)
                SkillAdminService.removeExperienceFromPlayer(client, user, id, xp)
                true
            } catch (_: Exception) {
                context.usage("Wrong usage.. ::${cmd[0]} id xp(0 - ${Int.MAX_VALUE} playername")
            }
        }
        context.alias == "pouch" && context.specialRights -> {
            if (client.freeSlots() >= 4) {
                client.addItem(5509, 1)
                for (i in 0 until 3) client.addItem(5510 + (i * 2), 1)
                client.checkItemUpdate()
            } else {
                context.reply("Need 4 free slots!")
            }
            return true
        }
        command.startsWith("update") && command.length > 7 && context.specialRights -> {
            Server.updateSeconds = command.substring(7).toInt() + 1
            Server.updateRunning = true
            Server.updateStartTime = System.currentTimeMillis()
            Server.trading = false
            Server.dueling = false
            return true
        }
        context.alias == "resettask" && context.specialRights -> {
            return try {
                val otherName = command.substring(cmd[0].length + 1)
                val otherIndex = PlayerHandler.getPlayerID(otherName)
                if (otherIndex != -1) {
                    val other = PlayerHandler.players[otherIndex] as Client
                    other.slayerData[3] = 0
                    other.send(SendMessage(client.playerName + " have reset your task!"))
                    context.reply("You reset the task for ${other.playerName}!")
                } else {
                    context.reply("Player $otherName is not online!")
                }
                true
            } catch (_: Exception) {
                context.usage("Try entering a name you want to tele to..")
            }
        }
        context.alias == "reloaditems" && context.specialRights -> {
            Server.itemManager.reloadItems()
            context.reply("You reloaded all items!")
            return true
        }
        context.alias == "setlevel" && context.specialRights -> {
            val skill = cmd[1].toInt()
            val level = cmd[2].toInt()
            if (level > 99 || level < 1) return true
            val skillType = Skill.getSkill(skill) ?: return true
            SkillAdminService.set(
                client = client,
                skill = skillType,
                level = level,
                experience = Skills.getXPForLevel(level),
            )
            if (skill == 3) {
                client.maxHealth = level
                client.heal(client.maxHealth)
            } else if (skill == 5) {
                client.maxPrayer = level
                client.currentPrayer = client.maxPrayer
                client.drainPrayer(0)
            }
            return true
        }
        context.alias == "setxp" && context.specialRights -> {
            val skill = cmd[1].toInt()
            val xp = cmd[2].toInt()
            val skillType = Skill.getSkill(skill) ?: return true
            if (xp + client.getExperience(skillType) > 200000000 || xp < 1) return true
            SkillAdminService.addXp(client, skillType, xp)
            SkillProgressionService.refresh(client, skillType)
            return true
        }
        command.equals("reset", true) && client.playerRights > 1 -> {
            SkillAdminService.reset(client)
            return true
        }
        command.startsWith("master") && client.playerRights > 1 -> {
            Skill.enabledSkills().forEach { skill -> SkillAdminService.addXp(client, skill, 14_000_000) }
            return true
        }
        context.alias == "quest" && client.playerRights > 1 -> {
            return try {
                val id = if (numericToken.matches(cmd[1])) cmd[1].toInt() else 0
                val amount = if (cmd.size > 2 && numericToken.matches(cmd[2])) cmd[2].toInt() else 1
                if (amount == 1) context.reply("quests = ${++client.quests[id]}")
                else {
                    client.quests[id] = amount
                    context.reply("quests = ${client.quests[id]}")
                }
                true
            } catch (_: Exception) {
                context.usage("wrong usage! ::quest id amount or ::quest id")
            }
        }
        context.alias == "quest_reward" && client.playerRights > 1 -> {
            client.send(SendString("Quest name here", 12144))
            client.send(SendString(" 99", 12147))
            for (i in 0 until 6) client.send(SendString(i.toString(), 12150 + i))
            client.sendFrame246(12145, 250, 4151)
            client.showInterface(12140)
            client.stillgfx(199, client.position.y, client.position.x)
            return true
        }
        context.alias == "moooo" && client.playerRights > 1 -> {
            client.clearQuestInterface()
            client.send(SendString("@str@testing something@str@", 8147))
            client.send(SendString("Yes", 8148))
            client.send(SendString("@369@Tits1@369@", 8149))
            client.send(SendString("@mon@Tits3@mon@", 8150))
            client.send(SendString("@lre@Tits3@lre@", 8151))
            client.sendQuestSomething(8143)
            client.showInterface(8134)
            return true
        }
    }
    return false
}
