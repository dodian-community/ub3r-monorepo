package net.dodian.uber.game.content.items

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage

object NoveltyItemCombinationHandler {
    @JvmStatic
    fun handle(
        client: Client,
        itemUsed: Int,
        otherItem: Int,
        itemUsedSlot: Int,
        usedWithSlot: Int,
        knife: Boolean,
    ): Boolean {
        if (handleShinyKey(client, itemUsed, otherItem)) {
            client.checkItemUpdate()
            return true
        }
        if (handlePartyhatCombos(client, itemUsed, otherItem, itemUsedSlot, usedWithSlot, knife)) {
            client.checkItemUpdate()
            return true
        }
        if (handleCapeDyes(client, itemUsed, otherItem, itemUsedSlot, usedWithSlot)) {
            client.checkItemUpdate()
            return true
        }
        return false
    }

    private fun handleShinyKey(client: Client, itemUsed: Int, otherItem: Int): Boolean {
        if (itemUsed != 85 && otherItem != 85) {
            return false
        }
        val otherId = if (itemUsed == 85) otherItem else itemUsed
        return when (otherId) {
            1543, 1544 -> {
                if (!client.checkItem(otherId + 1)) {
                    client.deleteItem(85, 1)
                    client.addItem(otherId + 1, 1)
                    client.send(
                        SendMessage(
                            "Your key shine bright and turned your ${client.getItemName(85).lowercase()} into a ${client.getItemName(otherId + 1).lowercase()}",
                        ),
                    )
                } else {
                    client.send(
                        SendMessage(
                            "I already have a ${client.getItemName(otherId + 1).lowercase()} in ${if (client.playerHasItem(otherId + 1)) "my inventory!" else "my bank!"}",
                        ),
                    )
                }
                true
            }
            2382, 2383 -> {
                if (!client.checkItem(989) && (!client.checkItem(2382) || !client.checkItem(2383))) {
                    val transformed = if (otherId == 2382) 2383 else 2382
                    client.deleteItem(85, 1)
                    client.addItem(transformed, 1)
                    client.send(
                        SendMessage(
                            "Your key shine bright and turned your ${client.getItemName(85).lowercase()} into a ${client.getItemName(transformed).lowercase()}",
                        ),
                    )
                } else if (!client.checkItem(989) && client.checkItem(2382) && client.checkItem(2383)) {
                    client.sendMessage("You already have the crystals, perhaps you should combine them?")
                } else {
                    client.send(
                        SendMessage(
                            "I already have a ${client.getItemName(989).lowercase()} in ${if (client.playerHasItem(989)) "my inventory!" else "my bank!"}",
                        ),
                    )
                }
                true
            }
            else -> false
        }
    }

    private fun handlePartyhatCombos(
        client: Client,
        itemUsed: Int,
        otherItem: Int,
        itemUsedSlot: Int,
        usedWithSlot: Int,
        knife: Boolean,
    ): Boolean {
        val rainbowHat =
            Server.itemManager.getName(itemUsed).endsWith("partyhat") &&
                Server.itemManager.getName(otherItem).endsWith("partyhat") &&
                Server.itemManager.isNote(itemUsed) &&
                Server.itemManager.isNote(otherItem)
        if (rainbowHat) {
            var hasItems = true
            var id = 1038
            while (id <= 1048 && hasItems) {
                if (!client.playerHasItem(id)) {
                    hasItems = false
                }
                id += 2
            }
            if (hasItems) {
                id = 1038
                while (id <= 1048) {
                    client.deleteItem(id, 1)
                    id += 2
                }
                client.addItemSlot(11863, 1, itemUsedSlot)
            } else {
                client.sendMessage("You need one of each partyhat to combine it into the rainbow partyhat!")
            }
            return true
        }
        if (knife && (itemUsed == 11863 || otherItem == 11863)) {
            val slotRemain = 5 - client.getFreeSpace()
            if (slotRemain <= 0) {
                client.deleteItem(11863, 1)
                var id = 1038
                while (id <= 1048) {
                    client.addItem(id, 1)
                    id += 2
                }
                client.sendMessage("You gentle used the knife on the paper hat and cut it into different color partyhats.")
            } else {
                client.sendMessage("You need to have ${if (slotRemain == 1) "one" else slotRemain} empty slot${if (slotRemain != 1) "s" else ""} to tear the rainbow partyhat apart.")
            }
            return true
        }
        if ((itemUsed == 962 || otherItem == 962) && (itemUsed == 11863 || otherItem == 11863)) {
            client.deleteItem(962, if (itemUsed == 962) itemUsedSlot else usedWithSlot, 1)
            client.deleteItem(11863, if (itemUsed == 11863) itemUsedSlot else usedWithSlot, 1)
            client.addItemSlot(11862, 1, if (itemUsed == 11863) itemUsedSlot else usedWithSlot)
            return true
        }
        if (knife && (itemUsed == 11862 || otherItem == 11862)) {
            if (client.getFreeSpace() > 0) {
                val slot = if (itemUsed == 11862) itemUsedSlot else usedWithSlot
                client.deleteItem(11862, slot, 1)
                client.addItemSlot(11863, 1, slot)
                client.addItem(962, 1)
                client.sendMessage("You gentle used the knife on the paper hat and cut it into a cracker and a rainbow partyhat.")
            } else {
                client.sendMessage("You need to have atleast one space to tear the black partyhat apart!")
            }
            return true
        }
        return false
    }

    private fun handleCapeDyes(
        client: Client,
        itemUsed: Int,
        otherItem: Int,
        itemUsedSlot: Int,
        usedWithSlot: Int,
    ): Boolean {
        val dyes = arrayOf(
            intArrayOf(-1, 1019),
            intArrayOf(1763, 1007),
            intArrayOf(1765, 1023),
            intArrayOf(1767, 1021),
            intArrayOf(1769, 1031),
            intArrayOf(1771, 1027),
            intArrayOf(1773, 1029),
        )
        for (dye in dyes) {
            if (itemUsed == dye[0] || otherItem == dye[0]) {
                for (target in dyes) {
                    if ((itemUsed == dye[0] && otherItem == target[1]) || (otherItem == dye[0] && itemUsed == target[1])) {
                        if (target[1] != dye[1]) {
                            client.deleteItem(itemUsed, itemUsedSlot, 1)
                            client.deleteItem(otherItem, usedWithSlot, 1)
                            client.addItemSlot(dye[1], 1, if (itemUsed == target[1]) itemUsedSlot else usedWithSlot)
                        } else {
                            client.sendMessage("There is no point in using the same color as the cape!")
                        }
                        return true
                    }
                }
            }
        }
        return false
    }
}
