package net.dodian.uber.game.skills.herblore

import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.content.npc.HerbloreNpcDialogue
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.utilities.Utils

object HerbloreBatchService {
    @JvmStatic
    fun handleEnteredAmount(client: Client, enteredAmount: Int): Boolean {
        if (client.XinterfaceID != 4753) {
            return false
        }
        client.send(RemoveInterfaces())
        var amount = enteredAmount
        val slot = client.XremoveSlot - 1
        if (slot !in client.herbOptions.indices) {
            client.XinterfaceID = -1
            return true
        }
        val id = client.herbOptions[slot].id
        val legacyNpcId = DialogueService.activeNpcId(client)
        val npcId = if (legacyNpcId > 0) legacyNpcId else 4753
        val grimy = client.GetItemName(id).lowercase().contains("grimy")
        val coins = client.getInvAmt(995)

        if (grimy) {
            amount = minOf(amount, coins / 200, client.getInvAmt(id))
            if (amount > 0) {
                if (client.getInvAmt(id) <= amount) {
                    client.herbOptions.removeAt(slot)
                }
                var otherHerb = -1
                for (h in Utils.grimy_herbs.indices) {
                    if (id == client.GetNotedItem(Utils.grimy_herbs[h])) {
                        otherHerb = client.GetNotedItem(Utils.herbs[h])
                        break
                    }
                }
                client.deleteItem(995, amount * 200)
                client.deleteItem(id, amount)
                client.addItem(otherHerb, amount)
                client.checkItemUpdate()
                HerbloreNpcDialogue.showBatchResultAndContinue(client, npcId, "Here is your all of ", "$amount ${client.GetItemName(id).lowercase()}")
            } else {
                client.showNPCChat(npcId, 605, arrayOf("You need 1 herb and 200 coins", "for me to grind it for you."))
            }
        } else if (!client.playerHasItem(228)) {
            client.showNPCChat(npcId, 605, arrayOf("You need noted vial of water for me to do that!"))
        } else {
            var otherHerb = -1
            for (h in Utils.herb_unf.indices) {
                if (id == client.GetNotedItem(Utils.herb_unf[h])) {
                    otherHerb = client.GetNotedItem(Utils.herbs[h])
                    break
                }
            }
            val vials = client.getInvAmt(228)
            val herbs = client.getInvAmt(otherHerb)
            amount = minOf(amount, coins / 1_000, vials, herbs)
            if (amount > 0) {
                if (herbs <= amount) {
                    client.herbOptions.removeAt(slot)
                }
                client.deleteItem(995, amount * 1_000)
                client.deleteItem(228, amount)
                client.deleteItem(otherHerb, amount)
                client.addItem(id, amount)
                client.checkItemUpdate()
                HerbloreNpcDialogue.showBatchResultAndContinue(client, npcId, "Here is your all of ", "$amount ${client.GetItemName(id).lowercase()}")
            } else {
                client.showNPCChat(
                    npcId,
                    605,
                    arrayOf(
                        "You need atleast 1 herb, 1 vial of water and 1000 coins",
                        "for me to turn it into a unfinish potion.",
                    ),
                )
            }
        }
        client.XinterfaceID = -1
        return true
    }
}
