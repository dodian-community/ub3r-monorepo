package net.dodian.uber.game.content.commands.beta

import net.dodian.uber.game.engine.systems.interaction.commands.*

import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.systems.interaction.commands.CommandContent
import net.dodian.uber.game.engine.systems.interaction.commands.CommandContext
import net.dodian.uber.game.engine.systems.interaction.commands.commands
import net.dodian.uber.game.content.commands.dev.handleFarmTest
import net.dodian.uber.game.content.commands.dev.handleSkillBank
import net.dodian.uber.game.content.commands.dev.handleSkillSet
import net.dodian.uber.game.content.commands.dev.handleSkillTools
import net.dodian.uber.game.content.commands.dev.handleSlayerTest
import net.dodian.uber.game.model.entity.UpdateFlag
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.Skills
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.engine.systems.skills.SkillAdminService
import net.dodian.uber.game.content.skills.thieving.PyramidPlunder
import net.dodian.uber.game.engine.config.gameWorldId

object BetaOnlyCommands : CommandContent {
    override fun definitions() =
        commands {
            command(
                "rehp", "debug", "boost", "tool", "potato", "pnpc", "item", "bank", "b", "perk",
                "plunder", "p_start", "p_tele", "p_next", "barrows", "setup",
                "skillbank", "skillingbank", "skilltools", "skillset", "farmtest", "slayertest", "skillbook",
            ) {
                handleBeta(this)
            }
        }
}

private fun handleBeta(context: CommandContext): Boolean {
    val client = context.client
    val cmd = context.parts
    if (gameWorldId <= 1) {
        return false
    }
    when {
        context.alias == "rehp" && !context.specialRights -> {
            client.reloadHp = !client.reloadHp
            context.reply("You set reload hp as ${client.reloadHp}")
            return true
        }
        context.alias == "debug" -> {
            client.debug = !client.debug
            context.reply("You set debug as ${client.debug}")
            return true
        }
        context.alias == "boost" -> {
            client.boost(5 + (Skills.getLevelForExperience(client.getExperience(Skill.STRENGTH)) * 0.15).toInt(), Skill.STRENGTH)
            client.boost(5 + (Skills.getLevelForExperience(client.getExperience(Skill.DEFENCE)) * 0.15).toInt(), Skill.DEFENCE)
            client.boost(5 + (Skills.getLevelForExperience(client.getExperience(Skill.ATTACK)) * 0.15).toInt(), Skill.ATTACK)
            client.boost(4 + (Skills.getLevelForExperience(client.getExperience(Skill.RANGED)) * 0.12).toInt(), Skill.RANGED)
            return true
        }
        (context.alias == "tool" || context.alias == "potato") && client.playerRights > 0 -> {
            client.addItem(5733, 1)
            client.checkItemUpdate()
            context.reply("Here is your dev potato!")
            return true
        }
        context.alias == "pnpc" && client.playerRights > 0 -> {
            return try {
                val npcId = cmd[1].toInt()
                if (npcId <= 8195) {
                    client.isNpc = npcId >= 0
                    client.setPlayerNpc(if (npcId >= 0) npcId else -1)
                    client.updateFlags.setRequired(UpdateFlag.APPEARANCE, true)
                }
                context.reply(if (npcId > 8195) "Maximum 8195 in npc id!" else if (npcId >= 0) "Setting npc to ${client.playerNpc}" else "Setting you normal!")
                true
            } catch (_: Exception) {
                context.usage("Wrong usage.. ::${cmd[0]} npcid")
            }
        }
        context.alias == "item" -> {
            val newItemID = cmd[1].toInt()
            var newItemAmount = cmd[2].toInt()
            if (newItemID < 1 || newItemID > 22376) {
                context.reply("Stop pulling a River! Maximum itemid = 22376!")
                return true
            }
            if (Server.itemManager.isStackable(newItemID)) {
                if (client.freeSlots() <= 0 && !client.playerHasItem(newItemID)) context.reply("Not enough space in your inventory.")
                else client.addItem(newItemID, newItemAmount)
            } else {
                newItemAmount = minOf(newItemAmount, client.freeSlots())
                if (newItemAmount > 0) {
                    repeat(newItemAmount) { client.addItem(newItemID, 1) }
                } else {
                    context.reply("Not enough space in your inventory.")
                }
            }
            client.checkItemUpdate()
            return true
        }
        !context.specialRights && (context.alias == "bank" || context.alias == "b") -> {
            client.openUpBank()
            return true
        }
        context.alias == "perk" -> {
            context.reply("Checking perk....")
            val equipCheck = client.skillcapePerk(Skill.WOODCUTTING, false)
            val invCheck = client.skillcapePerk(Skill.WOODCUTTING, true)
            context.reply("result = $equipCheck, $invCheck")
            return true
        }
        context.alias == "plunder" -> {
            PyramidPlunder.start(client)
            return true
        }
        context.alias == "p_start" -> {
            client.transport(PyramidPlunder.startPosition())
            return true
        }
        context.alias == "p_tele" -> {
            PyramidPlunder.currentDoor()?.let(client::transport)
            return true
        }
        context.alias == "p_next" -> {
            PyramidPlunder.advanceRoom(client)
            context.reply("You are now at floor ${PyramidPlunder.roomNumber(client) + 1}")
            return true
        }
        context.alias == "barrows" -> {
            for (i in 0 until 8) client.addItem(4746 + (i * 2), 1)
            for (i in 0 until 16) client.addItem(4709 + (i * 2), 1)
            client.checkItemUpdate()
            context.reply("Here is your barrows pieces!")
            return true
        }
        context.alias == "setup" -> {
            for (i in 0 until 7) {
                val level = if (i == 3) 78 else 75
                val skill = Skill.getSkill(i) ?: continue
                SkillAdminService.set(
                    client = client,
                    skill = skill,
                    level = level,
                    experience = Skills.getXPForLevel(level),
                )
                if (i == 3) {
                    client.maxHealth = level
                    client.heal(level)
                } else if (i == 5) {
                    client.maxPrayer = level
                    client.pray(level)
                }
            }
            client.equipment[0] = 3751
            client.equipmentN[0] = 1
            client.equipment[1] = 1007
            client.equipmentN[1] = 1
            client.equipment[2] = 1725
            client.equipmentN[2] = 1
            client.equipment[3] = 4587
            client.equipmentN[3] = 1
            client.equipment[4] = 3140
            client.equipmentN[4] = 1
            client.equipment[5] = 1540
            client.equipmentN[5] = 1
            client.equipment[7] = 4087
            client.equipmentN[7] = 1
            client.equipment[9] = 7459
            client.equipmentN[9] = 1
            client.equipment[10] = 4129
            client.equipmentN[10] = 1
            for (i in 0 until 14) client.setEquipment(client.equipment[i], client.equipmentN[i], i)
            client.addItem(5733, 1)
            repeat(27) { client.addItem(385, 1) }
            client.checkItemUpdate()
            context.reply("Your setup is done!")
            return true
        }
        context.alias == "skillbank" || context.alias == "skillingbank" -> {
            return handleSkillBank(context)
        }
        context.alias == "skillbook" -> {
            if (!context.hasArgs(1)) {
                return context.usage("Usage: ::skillbook <skill>")
            }
            return handleSkillBank(context, context.parts[1])
        }
        context.alias == "skilltools" -> {
            return handleSkillTools(context)
        }
        context.alias == "skillset" -> {
            return handleSkillSet(context)
        }
        context.alias == "farmtest" -> {
            return handleFarmTest(context)
        }
        context.alias == "slayertest" -> {
            return handleSlayerTest(context)
        }
    }
    return false
}
