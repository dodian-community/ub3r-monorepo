package net.dodian.uber.game.commands

import net.dodian.extensions.sendMessage
import net.dodian.uber.game.Server
import net.dodian.uber.game.libraries.commands.CommandSender
import net.dodian.uber.game.libraries.commands.Player
import net.dodian.uber.game.libraries.commands.helpers.command
import net.dodian.utilities.RightsFlag
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3

fun devCommands() {
    command<KFunction3<CommandSender, Int, Int?, Unit>, Unit> {
        name = "item"
        description = "Spawns an item into your inventory"
        permissions = listOf(RightsFlag.Developer)
        onCommand = ::spawnItem
    }

    command<KFunction2<CommandSender, Int, Unit>, Unit> {
        name = "update"
        description = "Start a server update countdown."
        permissions = listOf(RightsFlag.Developer, RightsFlag.Administrator)
        onCommand = ::startUpdateTimer
    }

    // if (cmd[0].equals("tomato")) {
    // if (cmd[0].equalsIgnoreCase("if")) {
    // if (!specialRights && (cmd[0].equalsIgnoreCase("bank") || cmd[0].equalsIgnoreCase("b"))) {
    // if (cmd[0].equalsIgnoreCase("emote")) {
    // if (cmd[0].equalsIgnoreCase("gfx")) {
    // if (cmd[0].equalsIgnoreCase("head")) {
    // if (cmd[0].equalsIgnoreCase("skull") && client.playerRights > 1) {
    // if (cmd[0].equalsIgnoreCase("sound") && client.playerRights > 1) {
    // if (cmd[0].equalsIgnoreCase("debug")) {
}

fun startUpdateTimer(sender: CommandSender, seconds: Int) {
    Server.updateAnnounced = false
    Server.updateSeconds = seconds + 1
    Server.updateRunning = true
    Server.updateStartTime = System.currentTimeMillis()
    Server.trading = false
    Server.dueling = false
}

fun spawnItem(sender: CommandSender, itemId: Int, amount: Int?) {
    val player = sender.player

    if (itemId < 1 || itemId > 22376)
        return sender.sendMessage("Stop pulling a River! Maximum item ID is 22,376!")

    if ((Server.itemManager.isStackable(itemId) && player.freeSlots() <= 0 && !player.playerHasItem(itemId)) ||
        (!Server.itemManager.isStackable(itemId) && (amount ?: 1) > player.freeSlots())
    ) return sender.sendMessage("Not enough space in your inventory.")

    player.addItem(itemId, amount ?: 1)
}