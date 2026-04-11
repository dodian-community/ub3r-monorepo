package net.dodian.uber.game.content.commands.admin

import net.dodian.uber.game.systems.interaction.commands.*

import net.dodian.uber.game.systems.interaction.commands.CommandContent
import net.dodian.uber.game.systems.interaction.commands.CommandContext
import net.dodian.uber.game.systems.interaction.commands.commands
import net.dodian.uber.game.engine.config.gameWorldId

object BossCommands : CommandContent {
    override fun definitions() =
        commands {
            command("forcetask", "bosspawn", "bosstele") {
                handleBoss(this)
            }
        }
}

private fun handleBoss(context: CommandContext): Boolean {
    val client = context.client
    val command = context.rawCommand
    val cmd = context.parts
    val specialRights = context.specialRights
    if (context.alias == "forcetask" && (specialRights || gameWorldId > 1)) {
        return try {
            val taskId = cmd[1].toInt()
            val length = net.dodian.uber.game.content.skills.slayer.SlayerTaskDefinition.values().size - 1
            if (taskId < 0 || taskId > length) {
                context.reply("Task id out of bound! Can only be 0 - $length")
                return true
            }
            client.slayerData[0] = if (client.slayerData[0] == -1) 402 else client.slayerData[0]
            client.slayerData[1] = taskId
            client.slayerData[2] = 1337
            client.slayerData[3] = 1337
            val task = net.dodian.uber.game.content.skills.slayer.SlayerTaskDefinition.forOrdinal(taskId) ?: return false
            context.reply("[DEBUG]: You force the task to be 1337 of  ${task.textRepresentation} (${task})")
            true
        } catch (_: Exception) {
            context.usage("Wrong usage.. ::${cmd[0]} taskId")
        }
    }
    if (command.startsWith("bosspawn") && (gameWorldId > 1 || specialRights)) {
        val npcName = command.substring(cmd[0].length + 1).replace(" ", "_")
        when {
            npcName.equals(client.boss_name[0], true) -> client.respawnBoss(4130)
            npcName.equals(client.boss_name[1], true) || npcName.equals("abyssal", true) -> client.respawnBoss(2585)
            npcName.equals(client.boss_name[2], true) || npcName.equals("san", true) -> client.respawnBoss(3964)
            npcName.equals(client.boss_name[3], true) || npcName.equals("bkt", true) -> client.respawnBoss(4067)
            npcName.equals(client.boss_name[4], true) || npcName.equals("jungle", true) -> client.respawnBoss(1443)
            npcName.equals(client.boss_name[5], true) -> client.respawnBoss(3957)
            npcName.equals(client.boss_name[6], true) || npcName.equals("nech", true) -> client.respawnBoss(8)
            npcName.equals(client.boss_name[7], true) || npcName.equals("queen", true) -> client.respawnBoss(4922)
            npcName.equals(client.boss_name[8], true) || npcName.equals("kbd", true) -> client.respawnBoss(239)
            npcName.equals(client.boss_name[9], true) || npcName.equals("mourner", true) || npcName.equals("head", true) -> client.respawnBoss(5311)
            npcName.equals(client.boss_name[10], true) || npcName.equals("black", true) -> client.respawnBoss(1432)
            npcName.equals(client.boss_name[11], true) || npcName.equals("prime", true) -> client.respawnBoss(2266)
            npcName.equals(client.boss_name[12], true) -> client.respawnBoss(2261)
            npcName.equals(client.boss_name[13], true) || npcName.equals("jad", true) -> client.respawnBoss(3127)
            npcName.equals(client.boss_name[14], true) || npcName.equals("kq", true) -> client.respawnBoss(4303)
            npcName.equals(client.boss_name[15], true) || npcName.equals("kk", true) -> client.respawnBoss(4304)
            npcName.equals(client.boss_name[16], true) -> client.respawnBoss(6610)
            else -> context.reply("Could not find boss: $npcName")
        }
        return true
    }
    if (command.startsWith("bosstele") && (gameWorldId > 1 || specialRights)) {
        val npcName = command.substring(cmd[0].length + 1).replace(" ", "_")
        when {
            npcName.equals(client.boss_name[0], true) -> client.triggerTele(2543, 3091, 0, false)
            npcName.equals(client.boss_name[1], true) || npcName.equals("abyssal", true) -> client.triggerTele(2626, 3084, 0, false)
            npcName.equals(client.boss_name[2], true) || npcName.equals("san", true) -> client.triggerTele(2613, 9521, 0, false)
            npcName.equals(client.boss_name[3], true) || npcName.equals("bkt", true) -> client.triggerTele(2566, 9507, 0, false)
            npcName.equals(client.boss_name[4], true) || npcName.equals("jungle", true) -> client.triggerTele(2572, 9529, 0, false)
            npcName.equals(client.boss_name[5], true) -> client.triggerTele(2889, 3426, 0, false)
            npcName.equals(client.boss_name[6], true) || npcName.equals("nech", true) -> client.triggerTele(2698, 9771, 0, false)
            npcName.equals(client.boss_name[7], true) || npcName.equals("queen", true) -> client.triggerTele(2866, 9951, 0, false)
            npcName.equals(client.boss_name[8], true) || npcName.equals("kbd", true) -> client.triggerTele(3315, 9374, 0, false)
            npcName.equals(client.boss_name[9], true) || npcName.equals("mourner", true) || npcName.equals("head", true) -> client.triggerTele(2554, 3278, 0, false)
            npcName.equals(client.boss_name[10], true) || npcName.equals("black", true) -> client.triggerTele(2907, 9805, 0, false)
            npcName.equals(client.boss_name[11], true) || npcName.equals("prime", true) -> client.triggerTele(2905, 9727, 0, false)
            npcName.equals(client.boss_name[12], true) -> client.triggerTele(2776, 3206, 0, false)
            npcName.equals(client.boss_name[13], true) || npcName.equals("jad", true) -> client.triggerTele(2393, 5090, 0, false)
            npcName.equals(client.boss_name[14], true) || npcName.equals("kq", true) -> client.triggerTele(3489, 9495, 0, false)
            npcName.equals(client.boss_name[15], true) || npcName.equals("kk", true) -> client.triggerTele(1713, 9843, 0, false)
            npcName.equals(client.boss_name[16], true) -> client.triggerTele(3218, 9934, 0, false)
            else -> context.reply("Could not find boss spawn for: $npcName")
        }
        return true
    }
    return false
}
