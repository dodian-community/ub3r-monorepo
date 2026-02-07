package net.dodian.uber.game.content.items.events

import net.dodian.uber.game.content.items.ItemContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.utilities.Misc

object EventPackageItems : ItemContent {
    override val itemIds: IntArray = intArrayOf(6199, 12854, 6542, 11996, 13345, 13346, 11918)

    override fun onFirstClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        return when (itemId) {
            6199 -> {
                val rewards = intArrayOf(6856, 6857, 6859, 6861, 6860, 6858)
                client.deleteItem(6199, 1)
                client.addItem(rewards[Misc.random(rewards.size - 1)], 1)
                client.send(SendMessage("Thank you for waiting patiently on us, take this as a token of gratitude!"))
                true
            }

            12854 -> {
                val presentIds = intArrayOf(6542, 11996, 13345, 13346)
                var slotNeeded = 0
                for (presentId in presentIds) {
                    if (!client.playerHasItem(presentId)) {
                        slotNeeded++
                    }
                }
                if (client.freeSlots() < slotNeeded) {
                    client.send(SendMessage("You need atleast $slotNeeded free slot to open this!"))
                    true
                } else {
                    client.deleteItem(itemId, 1)
                    for (presentId in presentIds) {
                        client.addItem(presentId, 3 + Misc.random(6))
                    }
                    true
                }
            }

            6542, 11996, 13345, 13346 -> {
                if (client.freeSlots() < 1) {
                    client.send(SendMessage("You need atleast one free slot to open this!"))
                    true
                } else {
                    val randomEventItems = intArrayOf(12887, 12888, 12889, 12890, 12891, 13343, 13344, 13203)
                    client.deleteItem(itemId, 1)
                    client.addItem(11997, 55 + Misc.random(500))
                    if (Misc.chance(1000) == 1) {
                        val eventItemId = randomEventItems[Misc.random(randomEventItems.size - 1)]
                        client.addItem(eventItemId, 1)
                        client.send(SendMessage("You found something of interest!"))
                        client.yell(
                            client.playerName + " just found " + client.GetItemName(eventItemId)
                                .lowercase() + " in a " + client.GetItemName(itemId).lowercase() + "!"
                        )
                    }
                    true
                }
            }

            11918 -> {
                if (client.freeSlots() < 1) {
                    client.send(SendMessage("You need atleast one free slot to open this!"))
                    true
                } else {
                    val halloweenMasks = intArrayOf(1053, 1055, 1057)
                    client.deleteItem(itemId, 1)
                    val maskId = halloweenMasks[Misc.random(halloweenMasks.size - 1)]
                    client.addItem(maskId, 1)
                    client.send(SendMessage("You found a ${client.GetItemName(maskId).lowercase()}!"))
                    client.yell(
                        client.playerName + " just found " + client.GetItemName(maskId)
                            .lowercase() + " in a " + client.GetItemName(itemId).lowercase() + "!"
                    )
                    true
                }
            }

            else -> false
        }
    }
}
