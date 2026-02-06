package net.dodian.uber.game.content.items.cosmetics

import net.dodian.uber.game.content.items.ItemContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.utilities.Misc

object ToyItems : ItemContent {
    override val itemIds: IntArray = intArrayOf(4566, 13203)

    override fun onSecondClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        when (itemId) {
            13203 -> {
                val quotes = arrayOf(
                    "You are easily the spunkiest warrior alive!",
                    "Not a single soul can challenge your spunk!",
                    "You are clearly the most spunktastic in all the land!",
                    "Your might is spunktacular!",
                    "It's spunkalicious!",
                    "You... are... spunky!",
                    "You are too spunktacular to measure!",
                    "You are the real M.V.P. dude!",
                    "More lazier then Spunky is Ivan :D",
                )
                client.send(SendMessage(quotes[Misc.random(quotes.size - 1)]))
                return true
            }

            4566 -> {
                client.requestAnim(1835, 0)
                return true
            }
        }
        return false
    }
}

