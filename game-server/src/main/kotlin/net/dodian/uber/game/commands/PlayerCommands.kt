package net.dodian.uber.game.commands

import net.dodian.extensions.sendMessage
import net.dodian.uber.game.Server
import net.dodian.uber.game.libraries.commands.CommandSender
import net.dodian.uber.game.libraries.commands.Player
import net.dodian.uber.game.libraries.commands.helpers.command
import net.dodian.uber.game.model.ChatLine
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage
import net.dodian.uber.game.model.player.packets.outgoing.SendString
import net.dodian.utilities.RightsFlag
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2

fun playerCommands() {
    command<KFunction1<CommandSender, Unit>, Unit> {
        name = "players"
        description = "Shows online players"
        onCommand = ::showOnlinePlayers
    }

    command<KFunction2<CommandSender, String, Unit>, Unit> {
        name = "yell"
        description = "Send a message in chat that's visible globally."
        permissions = listOf(RightsFlag.Premium)
        onCommand = ::yell
    }

    // if (cmd[0].equalsIgnoreCase("max")) {
    // if (command.startsWith("commands")) {
    // if (cmd[0].equalsIgnoreCase("boss")) {
}

fun yell(sender: CommandSender, message: String) = with(sender.player) {
    if (!Server.chatOn)
        return@with sendMessage("Yell is currently disabled.")

    if (muted)
        return@with sendMessage("You're currently muted.")

    val prefix = when {
        playerRights == 1 -> "[Y]<col=0B610B>$playerName:"
        playerRights >= 2 -> "[Y]<col=FFFF00>$playerName:<col=0B610B>"
        else -> "[Y]<col=000000>$playerName:"
    }

    val formattedMessage = "$prefix ${message.apply { this[0].uppercase() }}"
    Server.chat.add(ChatLine(playerName, dbId, 1, formattedMessage, position.x, position.y))
    yell(formattedMessage)
}

fun showOnlinePlayers(sender: CommandSender) {
    val player = sender.player

    player.send(SendMessage("There are currently <col=006600>" + PlayerHandler.getPlayerCount() + "<col=0> players online!"))
    player.send(SendString("@dre@                    Uber 3.0", 8144))
    player.clearQuestInterface()
    player.send(SendString("@dbl@Online players: @blu@" + PlayerHandler.getPlayerCount() + "", 8145))

    var line = 8147
    PlayerHandler.players.forEach {
        if (it == null) return@forEach

        val text = when (it.playerRights) {
            1 -> "@blu@Mod"
            2 -> "@blu@Admin"
            else -> ""
        } + " @dbl@${it.playerName} @bla@(Level-${it.determineCombatLevel()} @bla@is ${it.positionName}"
        player.send(SendString(text, line))
        line++
    }

    player.sendQuestSomething(8143)
    player.showInterface(8134)
}