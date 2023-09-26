package net.dodian.content

import net.dodian.uber.game.libraries.commands.CommandSender
import net.dodian.uber.game.libraries.commands.helpers.command
import net.dodian.utilities.RightsFlag
import kotlin.reflect.KFunction3

command<KFunction3<CommandSender, Int, Int?, Unit>, Unit> {
    name = "item"
    description = "Spawns an item into your inventory"
    permissions = listOf(RightsFlag.Developer)
    onCommand = ::spawnItem
}

fun spawnItem(sender: CommandSender, itemId: Int, amount: Int?) {
    val player = sender.player

    if (itemId < 1 || itemId > 22376)
        return sender.sendMessage("Stop pulling a River! Maximum item ID is 22,376!")

    //if ((Server.itemManager.isStackable(itemId) && player.freeSlots() <= 0 && !player.playerHasItem(itemId)) ||
    //    (!Server.itemManager.isStackable(itemId) && (amount ?: 1) > player.freeSlots())
    //) return sender.sendMessage("Not enough space in your inventory.")

    //player.addItem(itemId, amount ?: 1)
}