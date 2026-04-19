package net.dodian.uber.game.content.commands.admin

import net.dodian.uber.game.systems.interaction.commands.*

import net.dodian.uber.game.Server
import net.dodian.uber.game.persistence.account.Login
import net.dodian.uber.game.model.entity.UpdateFlag
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.netty.listener.out.CameraReset
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendCamera
import net.dodian.uber.game.systems.world.player.PlayerRegistry

object StaffCommands : CommandContent {
    private val moderationAliases = setOf(
        "pnpc", "invis", "teleto", "kick", "teletome", "staffzone", "test_area", "busy",
        "camera", "creset", "slots", "checkbank", "checkinv", "banmac", "tradelock",
        "meeting", "alltome",
    )

    override fun definitions() =
        commands {
            command(
                "pnpc", "invis", "teleto", "kick", "teletome", "staffzone", "test_area", "busy",
                "camera", "creset", "slots", "checkbank", "checkinv", "banmac", "tradelock",
                "meeting", "alltome", "toggleyell", "togglepvp", "toggletrade", "toggleduel",
                "toggledrop", "toggleshop", "togglebank",
            ) {
                if (alias in moderationAliases) {
                    handleStaffModeration(this)
                } else {
                    handleWorldControl(this)
                }
            }
        }
}

private fun handleStaffModeration(context: CommandContext): Boolean {
    val client = context.client
    val command = context.rawCommand
    val cmd = context.parts
    val specialRights = context.specialRights
    if (context.alias == "pnpc") {
        if (!specialRights) {
            return false
        }
        return try {
            val npcId = cmd[1].toInt()
            if (npcId <= 8195) {
                client.isNpc = npcId >= 0
                client.setPlayerNpc(if (npcId >= 0) npcId else -1)
                client.updateFlags.setRequired(UpdateFlag.APPEARANCE, true)
            }
            client.sendMessage(if (npcId > 8195) "Maximum 8195 in npc id!" else if (npcId >= 0) "Setting npc to ${client.playerNpc}" else "Setting you normal!")
            true
        } catch (_: Exception) {
            context.usage("Wrong usage.. ::${cmd[0]} npcid")
        }
    }
    if (client.playerRights <= 0) {
        return false
    }
    when {
        context.alias == "invis" -> {
            client.invis = !client.invis
            client.sendMessage("You turn invis to ${client.invis}")
            client.transport(client.position)
            recordStaffCommand(client, command)
            return true
        }
        context.alias == "teleto" -> {
            return try {
                if (!canUseStaffTeleport(client, specialRights)) {
                    client.sendMessage("Command can't be used in the wilderness!")
                    return true
                }
                val otherName = context.playerNameTail()
                val otherIndex = PlayerRegistry.getPlayerID(otherName)
                if (otherIndex != -1) {
                    val other = PlayerRegistry.players[otherIndex] as Client
                    if (other.wildyLevel > 0 && !specialRights) {
                        client.sendMessage("That player is in the wilderness!")
                        return true
                    }
                    if (client.UsingAgility || other.UsingAgility || System.currentTimeMillis() < client.walkBlock) {
                        return true
                    }
                    client.transport(other.position.copy())
                    client.sendMessage("Teleto: You teleport to ${other.playerName}")
                    recordStaffCommand(client, command)
                } else {
                    client.sendMessage("Player $otherName is not online!")
                }
                true
            } catch (_: Exception) {
                context.usage("Try entering a name you want to tele to..")
            }
        }
        context.alias == "kick" -> {
            return try {
                val otherName = context.playerNameTail()
                val otherIndex = PlayerRegistry.getPlayerID(otherName)
                if (otherIndex != -1) {
                    val other = PlayerRegistry.players[otherIndex] as Client
                    other.disconnected = true
                    client.sendMessage("Player ${other.playerName} has been kicked!")
                    recordStaffCommand(client, command)
                } else {
                    client.sendMessage("Player $otherName is not online!")
                }
                true
            } catch (exception: Exception) {
                client.sendMessage("Try entering a name you wish to kick..")
                client.sendMessage(exception.message)
                true
            }
        }
        context.alias == "teletome" -> {
            return try {
                if (!canUseStaffTeleport(client, specialRights)) {
                    client.sendMessage("Command can't be used in the wilderness")
                    return true
                }
                val otherName = context.playerNameTail()
                val otherIndex = PlayerRegistry.getPlayerID(otherName)
                if (otherIndex != -1) {
                    val other = PlayerRegistry.players[otherIndex] as Client
                    if (other.wildyLevel > 0 && !specialRights) {
                        client.sendMessage("Can not teleport someone out of the wilderness! Contact a admin!")
                        return true
                    }
                    if (client.UsingAgility || other.UsingAgility || System.currentTimeMillis() < client.walkBlock) {
                        return true
                    }
                    other.transport(client.position.copy())
                    recordStaffCommand(client, command)
                } else {
                    client.sendMessage("Player $otherName is not online!")
                }
                true
            } catch (_: Exception) {
                context.usage("Try entering a name you want to tele to you..")
            }
        }
        context.alias == "staffzone" -> {
            if (client.inWildy()) {
                client.sendMessage("Cant use this in the wilderness!")
                return true
            }
            client.teleportTo(2936, 4688, 0)
            client.sendMessage("Welcome to the staff zone!")
            return true
        }
        context.alias == "test_area" -> {
            client.triggerTele(3260, 2784, 0, false)
            client.sendMessage("Welcome to the monster test area!")
            return true
        }
        context.alias == "busy" && client.playerRights > 1 -> {
            client.busy = !client.busy
            client.sendMessage(if (!client.busy) "You are no longer busy!" else "You are now busy!")
            return true
        }
        context.alias == "camera" -> {
            client.send(SendCamera("rotation", client.position.x, client.position.y, 100, 2, 2, ""))
            return true
        }
        context.alias == "creset" -> {
            client.send(CameraReset())
            return true
        }
        context.alias == "slots" -> {
            if (client.playerRights < 2) {
                client.sendMessage("Do not fool with yaaaaar!")
                return true
            }
            client.send(RemoveInterfaces())
            client.openInterface(671)
            Server.slots.playSlots(client, -1)
            return true
        }
        context.alias == "checkbank" -> {
            client.openUpOtherBank(context.playerNameTail())
            recordStaffCommand(client, command)
            return true
        }
        context.alias == "checkinv" -> {
            client.openUpOtherInventory(context.playerNameTail())
            recordStaffCommand(client, command)
            return true
        }
        command.startsWith("banmac") -> {
            return try {
                val otherName = command.substring(7)
                val otherIndex = PlayerRegistry.getPlayerID(otherName)
                if (otherIndex != -1) {
                    val other = PlayerRegistry.players[otherIndex] as Client
                    Login.addUidToFile(other.UUID)
                    other.logout()
                    recordStaffCommand(client, command)
                } else {
                    client.sendMessage("Error MAC banning player. Name doesn't exist or player is offline.")
                }
                true
            } catch (_: Exception) {
                context.usage("Invalid Syntax! Use as ::banmac PlayerName")
            }
        }
        command.startsWith("tradelock") -> {
            return try {
                if (client.wildyLevel > 0) {
                    client.sendMessage("Command can't be used in the wilderness")
                    return true
                }
                val otherName = context.playerNameTail()
                val otherIndex = PlayerRegistry.getPlayerID(otherName)
                if (otherIndex != -1) {
                    val other = PlayerRegistry.players[otherIndex] as Client
                    other.tradeLocked = true
                    client.sendMessage("You have just tradelocked $otherName")
                    recordStaffCommand(client, command)
                } else {
                    client.sendMessage("The name doesnt exist.")
                }
                true
            } catch (_: Exception) {
                context.usage("Try entering a name you want to tradelock..")
            }
        }
        command.equals("meeting", true) && client.playerRights > 1 -> {
            for (i in PlayerRegistry.players.indices) {
                if (client.validClient(i)) {
                    val other = client.getClient(i)
                    if (other.playerRights > 0) {
                        other.sendMessage("All of you belong to ${client.playerName}")
                        other.triggerTele(2936, 4688, 0, false)
                    }
                }
            }
            return true
        }
        command.equals("alltome", true) && client.playerRights > 1 -> {
            for (i in PlayerRegistry.players.indices) {
                if (client.validClient(i)) {
                    val other = client.getClient(i)
                    if (other == client) continue
                    other.sendMessage("<col=cc0000>A force moved you towards a location!")
                    other.triggerTele(client.position.x, client.position.y, client.position.z, false)
                }
            }
            client.sendMessage("You teleported all online to you!")
            return true
        }
    }
    return true
}

private fun handleWorldControl(context: CommandContext): Boolean {
    val client = context.client
    if (client.playerRights <= 0) {
        return false
    }
    val toggleCommand = if (context.alias.startsWith("toggle")) context.alias.replace("_", "") else context.alias
    when {
        toggleCommand.equals("toggleyell", true) -> {
            Server.chatOn = !Server.chatOn
            client.yell(if (Server.chatOn) "[SERVER]: Yell has been enabled!" else "[SERVER]: Yell has been disabled!")
        }
        toggleCommand.equals("togglepvp", true) -> {
            Server.pking = !Server.pking
            client.yell(if (Server.pking) "[SERVER]: Player Killing has been enabled!" else "[SERVER]: Player Killing  has been disabled!")
        }
        toggleCommand.equals("toggletrade", true) -> {
            Server.trading = !Server.trading
            client.yell(if (Server.trading) "[SERVER]: Trading has been enabled!" else "[SERVER]: Trading has been disabled!")
        }
        toggleCommand.equals("toggleduel", true) -> {
            Server.dueling = !Server.dueling
            client.yell(if (Server.dueling) "[SERVER]: Dueling has been enabled!" else "[SERVER]: Dueling has been disabled!")
        }
        toggleCommand.equals("toggledrop", true) -> {
            Server.dropping = !Server.dropping
            client.yell(if (Server.dropping) "[SERVER]: Dropping items has been enabled!" else "[SERVER]: Dropping items has been disabled!")
        }
        toggleCommand.equals("toggleshop", true) -> {
            Server.shopping = !Server.shopping
            client.yell(if (Server.shopping) "[SERVER]: Shops has been enabled!" else "[SERVER]: Shops has been disabled!")
        }
        toggleCommand.equals("togglebank", true) -> {
            Server.banking = !Server.banking
            client.yell(if (Server.banking) "[SERVER]: The Bank has been enabled!" else "[SERVER]: The Bank has been disabled!")
            if (!Server.banking) {
                for (player in PlayerRegistry.players) {
                    val other = player as? Client ?: continue
                    if (other.IsBanking) {
                        other.send(RemoveInterfaces())
                        other.IsBanking = false
                    }
                }
            }
        }
        else -> return false
    }
    recordStaffCommand(client, context.rawCommand)
    return true
}
