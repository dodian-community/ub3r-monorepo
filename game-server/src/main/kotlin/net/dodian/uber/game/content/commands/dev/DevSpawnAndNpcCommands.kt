package net.dodian.uber.game.content.commands

import java.text.DecimalFormat
import net.dodian.uber.game.Server
import net.dodian.uber.game.content.commands.CommandContent
import net.dodian.uber.game.content.commands.CommandContext
import net.dodian.uber.game.content.commands.commandLogger
import net.dodian.uber.game.content.commands.commands
import net.dodian.uber.game.model.entity.npc.NpcData
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.world.player.PlayerRegistry
import net.dodian.uber.game.model.UpdateFlag
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.persistence.command.CommandDbService
import net.dodian.uber.game.engine.config.gameWorldId

object DevSpawnAndNpcCommands : CommandContent {
    override fun definitions() =
        commands {
            command("tnpc", "findnpc", "telemob", "findmob", "npc_data", "r_drops", "d_drop", "a_drop", "droptable", "addnpc", "addxmastree") {
                handleDevNpc(this)
            }
        }
}

private fun handleDevNpc(context: CommandContext): Boolean {
    val client = context.client
    val command = context.rawCommand
    val cmd = context.parts
    if (context.alias == "tnpc" && context.specialRights && gameWorldId > 1) {
        return try {
            val id = cmd[1].toInt()
            val pos = client.position.copy()
            Server.npcManager.createNpc(id, pos, 0)
            context.reply("Npc temporary spawned = $id, at x = ${pos.x} y = ${pos.y} with height ${pos.z}")
            true
        } catch (_: Exception) {
            context.usage("Wrong usage.. ::${cmd[0]} npcid")
        }
    }
    if (context.alias == "findnpc" && context.specialRights) {
        return try {
            val id = cmd[1].toInt()
            var found = 0
            var shown = 0
            for (npc in Server.npcManager.npcMap.values) {
                if (npc.id != id) continue
                found++
                if (shown < 20) {
                    val pos = npc.position
                    context.reply("Npc $id at (${pos.x}, ${pos.y}, ${pos.z})")
                    shown++
                }
            }
            when {
                found == 0 -> context.reply("No loaded NPCs found with id $id.")
                found > shown -> context.reply("Found $found NPCs with id $id (showing $shown).")
                else -> context.reply("Found $found NPCs with id $id.")
            }
            true
        } catch (_: Exception) {
            context.usage("Wrong usage.. ::findnpc npcId")
        }
    }
    if (command.startsWith("telemob") && context.specialRights) {
        val mobId = cmd[1].toInt()
        for (npc in Server.npcManager.npcMap.values) {
            if (npc.id == mobId) {
                client.triggerTele(npc.position.x, npc.position.y, npc.position.z, false)
                return true
            }
        }
        return true
    }
    if (command.startsWith("findmob") && context.specialRights) {
        val npcName = command.substring(cmd[0].length + 1).replace("_", " ")
        for (npc in Server.npcManager.npcMap.values) {
            val npcCheckName = npc.npcName().replace("_", " ")
            if (npcName.equals(npcCheckName, true)) {
                context.reply("Found $npcCheckName (${npc.id}) at ${npc.position}")
                return true
            }
        }
        return true
    }
    if (!context.specialRights) {
        return false
    }
    when (context.alias) {
        "npc_data" -> {
            if (client.playerNpc < 0) {
                context.reply("please try to do ::pnpc id")
                return true
            }
            return try {
                val data = cmd[1]
                val value = command.substring(cmd[0].length + cmd[1].length + 2)
                Server.npcManager.reloadNpcConfig(client, client.playerNpc, data, value)
                true
            } catch (_: Exception) {
                context.usage("Wrong usage.. ::${cmd[0]} config value")
            }
        }
        "r_drops" -> {
            return try {
                val id = if (client.playerNpc < 1) cmd[1].toInt() else client.playerNpc
                Server.npcManager.reloadDrops(client, id)
                true
            } catch (_: Exception) {
                context.usage("Wrong usage.. ::${cmd[0]} id")
            }
        }
        "d_drop" -> {
            if (client.playerNpc < 0) {
                context.reply("please try to do ::pnpc id")
                return true
            }
            val itemId = cmd[1].toInt()
            val chance = cmd[2].toDouble()
            val npcId = client.playerNpc
            CommandDbService.submit(
                "delete-npc-drop",
                { CommandDbService.deleteNpcDrop(npcId, itemId, chance) },
                { result ->
                    if (!client.disconnected) {
                        if (result.updatedRows < 1) {
                            context.reply("${Server.npcManager.getName(npcId)} does not have the ${client.getItemName(itemId)} with the chance $chance% !")
                        } else {
                            context.reply("You deleted ${client.getItemName(itemId)} drop with the chance of $chance% from ${Server.npcManager.getName(npcId)}")
                        }
                    }
                },
                { exception ->
                    if (!client.disconnected) {
                        context.reply("Wrong usage.. ::d_drop itemid chance")
                        commandLogger.debug("delete-npc-drop failed for npcId={} itemId={}", npcId, itemId, exception)
                    }
                },
            )
            return true
        }
        "a_drop" -> {
            if (client.playerNpc < 0) {
                context.reply("please try to do ::pnpc id")
                return true
            }
            return try {
                val itemId = cmd[1].toInt()
                val min = cmd[2].toInt()
                val max = cmd[3].toInt()
                val numberFormat = DecimalFormat("###.###")
                val first = if (!cmd[4].contains(":")) 0.0 else cmd[4].split(":")[0].toDouble()
                val second = if (!cmd[4].contains(":")) 0.0 else cmd[4].split(":")[1].toDouble()
                var chance = if (first != 0.0 || second != 0.0) numberFormat.format((first / second) * 100).toDouble() else cmd[4].toDouble()
                chance = if (chance > 100.000) 100.0 else maxOf(chance, 0.001)
                val rareShout = if (cmd.size >= 6 && (cmd[5].equals("false", true) || cmd[5].equals("true", true))) cmd[5].lowercase() else "false"
                val npcId = client.playerNpc
                CommandDbService.submit(
                    "add-npc-drop",
                    { CommandDbService.insertNpcDrop(npcId, chance, itemId, min, max, rareShout) },
                    { _ ->
                        if (!client.disconnected) {
                            context.reply("You added $min-$max ${client.getItemName(itemId)} to ${Server.npcManager.getName(npcId)} with a chance of $chance%${if (rareShout == "true") " with a yell!" else ""}")
                        }
                    },
                    { exception ->
                        if (!client.disconnected) {
                            if (exception.message?.contains("Duplicate entry") == true) {
                                context.reply("${client.getItemName(itemId)} with the chance of $chance% already exist for the ${Server.npcManager.getName(npcId)}")
                            } else {
                                context.reply("Something bad happend with sql!")
                                commandLogger.debug("add-npc-drop failed for npcId={} itemId={}", npcId, itemId, exception)
                            }
                        }
                    },
                )
                true
            } catch (_: Exception) {
                context.usage("Wrong usage.. ::${cmd[0]} itemid min max procent(x:y or %)")
            }
        }
        "droptable" -> {
            if (client.playerNpc < 0) {
                context.reply("please try to do ::pnpc id")
                return true
            }
            val npcId = client.playerNpc
            val npcData: NpcData = Server.npcManager.getData(npcId) ?: run {
                context.reply("Npc with id $npcId is missing from definitions.")
                return true
            }
            if (!npcData.drops.isEmpty()) {
                context.reply("-----------DROPS FOR ${Server.npcManager.getName(client.playerNpc).uppercase()}-----------")
                for (drop in npcData.drops) {
                    context.reply("${drop.minAmount} - ${drop.maxAmount} ${client.getItemName(drop.id)}(${drop.id}) ${drop.chance}%")
                }
            } else {
                context.reply("Npc ${npcData.name} ($npcId) has no assigned drops!")
            }
        }
        "addxmastree" -> {
            val x = client.position.x
            val y = client.position.y
            CommandDbService.submit(
                "add-xmas-tree",
                { CommandDbService.insertObjectDefinition(1318, x, y, 2) },
                { _ -> if (!client.disconnected) context.reply("Object added, at x = $x y = $y") },
                { exception -> commandLogger.debug("add-xmas-tree failed at x={} y={}", x, y, exception) },
            )
        }
        "addnpc" -> {
            context.reply("This command is disabled after the NPC spawn hard cutover.")
            context.reply("Add NPC spawns in Kotlin content files under content/npcs for permanent changes.")
        }
        else -> return false
    }
    return true
}
