package net.dodian.uber.game.command.player

import net.dodian.uber.game.engine.systems.interaction.commands.*

import net.dodian.uber.game.Server
import net.dodian.uber.game.combat.getRangedStr
import net.dodian.uber.game.combat.magicBonusDamage
import net.dodian.uber.game.combat.meleeMaxHit
import net.dodian.uber.game.combat.rangedMaxHit
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendString
import net.dodian.uber.game.engine.config.gameWorldId

object PlayerCommands : CommandContent {
    override fun definitions() =
        commands {
            command(
                "request", "report", "suggest", "bug", "rules", "droplist", "drops", "latestclient",
                "news", "thread", "highscores", "price", "max", "players", "noclip", "slay_sim",
            ) {
                handlePlayerUtility(this)
            }
        }
}

private fun handlePlayerUtility(context: CommandContext): Boolean {
    val client = context.client
    val command = context.rawCommand
    val cmd = context.parts
    when {
        context.alias == "request" -> Player.openPage(client, "https://dodian.net/forumdisplay.php?f=83")
        context.alias == "report" -> Player.openPage(client, "https://dodian.net/forumdisplay.php?f=118")
        context.alias == "suggest" -> Player.openPage(client, "https://dodian.net/forumdisplay.php?f=4")
        context.alias == "bug" -> Player.openPage(client, "https://dodian.net/forumdisplay.php?f=120")
        context.alias == "rules" -> Player.openPage(client, "https://dodian.net/index.php?pageid=rules")
        context.alias == "droplist" || (context.alias == "drops" && client.playerRights < 2) ->
            Player.openPage(client, "https://dodian.net/index.php?pageid=droplist")
        context.alias == "latestclient" -> Player.openPage(client, "https://dodian.net/client/DodianClient.jar")
        context.alias == "news" -> Player.openPage(client, "https://dodian.net/showthread.php?t=${client.latestNews}")
    }
    if (context.alias == "thread") {
        return try {
            Player.openPage(client, "https://dodian.net/showthread.php?t=${cmd[1].toInt()}")
            true
        } catch (_: Exception) {
            context.usage("Wrong usage.. ::${cmd[0]} page")
        }
    }
    if (context.alias == "highscores") {
        return try {
            val firstPerson = if (cmd.size < 2) "" else cmd[1].replace("_", "+")
            val secondPerson = if (cmd.size < 3) "" else cmd[2].replace("_", "+")
            val url =
                if (firstPerson.isEmpty() && secondPerson.isEmpty()) {
                    "https://dodian.net/index.php?pageid=highscores"
                } else if (firstPerson.isNotEmpty() && secondPerson.isEmpty()) {
                    "https://dodian.net/index.php?pageid=highscores&player1=$firstPerson"
                } else {
                    "https://dodian.net/index.php?pageid=highscores&player1=$firstPerson&player2=$secondPerson"
                }
            Player.openPage(client, url)
            true
        } catch (_: Exception) {
            client.sendMessage("Wrong usage.. ::${cmd[0]} or ::${cmd[0]} First_name or")
            client.sendMessage("::${cmd[0]} First_name second_name")
            true
        }
    }
    if (command.startsWith("noclip") && client.playerRights < 2 && gameWorldId == 1) {
        client.disconnected = true
        return true
    }
    if (command.startsWith("slay_sim") && gameWorldId > 1) {
        val taskStreak = intArrayOf(1000, 500, 250, 100, 50, 10)
        val experience = intArrayOf(50, 30, 20, 11, 6, 2)
        var totalTimes = 0
        for (task in 1..1000) {
            var bonusXp = -1
            for (i in taskStreak.indices) {
                if (bonusXp == -1 && task % taskStreak[i] == 0) {
                    totalTimes += experience[i]
                    bonusXp = 0
                }
            }
        }
        client.sendMessage("Total amount of times: $totalTimes out of 1000!")
        return true
    }
    if (context.alias == "price") {
        val name = command.substring(cmd[0].length + 1)
        Server.itemManager.getItemName(client, name)
        return true
    }
    if (context.alias == "max") {
        client.sendMessage("<col=FF8000>Melee max hit: ${client.meleeMaxHit()} (MeleeStr: ${client.playerBonus[10]})")
        client.sendMessage("<col=0B610B>Range max hit: ${client.rangedMaxHit()} (RangeStr: ${client.getRangedStr()})")
        val magicIncrease = String.format("%3.1f", (client.magicBonusDamage() - 1.0) * 100.0)
        if (client.autocast_spellIndex == -1) {
            client.sendMessage("<col=292BA3>Magic max hit (smoke rush): ${(client.baseDamage[0] * client.magicBonusDamage()).toInt()} (Magic damage increase: $magicIncrease%)")
        } else {
            client.sendMessage("<col=292BA3>Magic max hit (${client.spellName[client.autocast_spellIndex]}): ${(client.baseDamage[client.autocast_spellIndex] * client.magicBonusDamage()).toInt()} (Magic damage increase: $magicIncrease%)")
        }
        return true
    }
    if (command.equals("players", true)) {
        client.sendMessage("There are currently <col=006600>${PlayerRegistry.getPlayerCount()}<col=0> players online!")
        client.sendString("@dre@                    Uber 3.0", 8144)
        client.clearQuestInterface()
        client.sendString("@dbl@Online players: @blu@${PlayerRegistry.getPlayerCount()}", 8145)
        var line = 8147
        var count = 0
        for (player in PlayerRegistry.players) {
            if (player != null && player.dbId >= 0) {
                val title =
                    when {
                        player.playerRights == 1 && player.playerGroup == 5 -> "@blu@Mod "
                        player.playerRights == 1 && player.playerGroup == 9 -> "@blu@Trial Mod "
                        player.playerRights == 2 && player.playerGroup == 10 -> "@yel@Developer "
                        player.playerRights == 2 -> "@yel@Admin "
                        else -> ""
                    }
                client.sendString("@bla@$title@dbl@${player.playerName} @bla@(Level-${player.determineCombatLevel()}) @bla@is ${player.positionName}", line)
                line++
                count++
                if (line == 8196) {
                    line = 12174
                }
                if (count > 100) {
                    break
                }
            }
        }
        if (PlayerRegistry.getPlayerCount() > 100) {
            client.sendMessage("Note: there are too many players online to list, 100 are shown")
        }
        client.sendQuestSomething(8143)
        client.openInterface(8134)
        return true
    }
    return true
}
