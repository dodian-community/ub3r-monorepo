package net.dodian.uber.game.commands

import net.dodian.extensions.sendMessage
import net.dodian.uber.game.libraries.commands.CommandSender
import net.dodian.uber.game.libraries.commands.Player
import net.dodian.uber.game.libraries.commands.helpers.command
import net.dodian.utilities.RightsFlag
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3

fun staffCommands() {
    command<KFunction2<CommandSender, Player, Unit>, Unit> {
        name = "tradelock"
        description = "Disables player from trading, dropping, or selling items."
        permissions = listOf(RightsFlag.Developer, RightsFlag.Administrator, RightsFlag.Moderator)
        onCommand = ::tradeLock
    }

    command<KFunction2<CommandSender, Player, Unit>, Unit> {
        name = "kick"
        description = "Kicks a player back to the login screen."
        permissions = listOf(RightsFlag.Developer, RightsFlag.Administrator, RightsFlag.Moderator)
        onCommand = ::kick
    }

    command<KFunction3<CommandSender, Player, Int?, Unit>, Unit> {
        name = "ban"
        description = "Disables player from trading, dropping, or selling items"
        permissions = listOf(RightsFlag.Developer, RightsFlag.Administrator, RightsFlag.Moderator)
        onCommand = ::ban
    }

    command<KFunction3<CommandSender, Player, Int?, Unit>, Unit> {
        name = "mute"
        description = "Disables player from trading, dropping, or selling items"
        permissions = listOf(RightsFlag.Developer, RightsFlag.Administrator, RightsFlag.Moderator)
        onCommand = ::mute
    }

    command<KFunction2<CommandSender, Player, Unit>, Unit> {
        name = "teleto"
        description = "Disables player from trading, dropping, or selling items"
        permissions = listOf(RightsFlag.Developer, RightsFlag.Administrator, RightsFlag.Moderator)
        onCommand = ::tradeLock
    }

    command<KFunction2<CommandSender, Player, Unit>, Unit> {
        name = "teletome"
        description = "Disables player from trading, dropping, or selling items"
        permissions = listOf(RightsFlag.Developer, RightsFlag.Administrator, RightsFlag.Moderator)
        onCommand = ::tradeLock
    }

    command<KFunction2<CommandSender, Player, Unit>, Unit> {
        name = "tele"
        description = "Disables player from trading, dropping, or selling items"
        permissions = listOf(RightsFlag.Developer, RightsFlag.Administrator, RightsFlag.Moderator)
        onCommand = ::tradeLock
    }

    command<KFunction1<CommandSender, Unit>, Unit> {
        name = "invis"
        description = "Go invisible - means other players won't see you."
        permissions = listOf(RightsFlag.Developer, RightsFlag.Administrator, RightsFlag.Moderator)
        onCommand = ::goInvisible
    }

    // if (command.equalsIgnoreCase("meeting") && client.playerRights > 1) {
    // if (command.equalsIgnoreCase("alltome") && client.playerRights > 1) {
    // if (cmd[0].equalsIgnoreCase("staffzone")) {
    // if (client.playerRights > 0) { //Toggle commands!
}

fun mute(sender: CommandSender, player: Player, hours: Int?) = with(sender.player) {
    player.muted = true
    player.mutedTill = (System.currentTimeMillis() / 1000) + (60 * 60 * (hours ?: 24))

    sendMessage("You have muted ${player.playerName} for ${hours ?: 24} hours!")
    player.sendMessage("$playerName has muted you for ${hours ?: 24} hours!")
}

fun kick(sender: CommandSender, player: Player) = with(sender.player) {
    player.kick()
    player.logout()
    sendMessage("You've just kicked ${player.playerName}!")
}

fun ban(sender: CommandSender, player: Player, hours: Int?) = with(sender.player) {
    sendMessage("This command is not yet implemented.")
}

fun goInvisible(sender: CommandSender) = with(sender.player) {
    when (invis) {
        true -> {
            invis = false
            sendMessage("You're now visible to others again!")
        }

        false -> {
            invis = true
            sendMessage("You're now invisible to others!")
        }
    }
}

fun tradeLock(sender: CommandSender, player: Player) {
    player.tradeLocked = true
    sender.sendMessage("You've just disabled trading for ${player.playerName}")
}