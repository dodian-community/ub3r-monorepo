package net.dodian.uber.game.content.skills.herblore

import net.dodian.uber.game.systems.ui.dialogue.DialogueService
import net.dodian.uber.game.content.npcs.spawns.HerbloreNpcDialogue
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces

object HerbloreService {
    @JvmStatic
    fun handleEnteredAmount(client: Client, enteredAmount: Int): Boolean {
        if (client.XinterfaceID != HerbloreDefinitions.BATCH_INTERFACE_ID) {
            return false
        }
        client.send(RemoveInterfaces())
        val slot = client.XremoveSlot - 1
        if (slot !in client.herbOptions.indices) {
            client.XinterfaceID = -1
            return true
        }
        val legacyNpcId = DialogueService.activeNpcId(client)
        val request = HerbloreBatchRequest(
            slot = slot,
            amount = enteredAmount,
            npcId = if (legacyNpcId > 0) legacyNpcId else HerbloreDefinitions.BATCH_INTERFACE_ID,
        )
        processBatch(client, request)
        client.XinterfaceID = -1
        return true
    }

    @JvmStatic
    fun processBatch(client: Client, request: HerbloreBatchRequest) {
        var amount = request.amount
        val id = client.herbOptions[request.slot].id
        val grimy = client.GetItemName(id).lowercase().contains("grimy")
        val coins = client.getInvAmt(995)

        if (grimy) {
            amount = minOf(amount, coins / HerbloreDefinitions.GRIND_COST_PER_HERB, client.getInvAmt(id))
            if (amount > 0) {
                if (client.getInvAmt(id) <= amount) {
                    client.herbOptions.removeAt(request.slot)
                }
                val otherHerb =
                    HerbloreDefinitions.herbDefinitions
                        .firstOrNull { id == client.GetNotedItem(it.grimyId) }
                        ?.let { client.GetNotedItem(it.cleanId) }
                        ?: -1
                client.deleteItem(995, amount * HerbloreDefinitions.GRIND_COST_PER_HERB)
                client.deleteItem(id, amount)
                client.addItem(otherHerb, amount)
                client.checkItemUpdate()
                HerbloreNpcDialogue.showBatchResultAndContinue(client, request.npcId, "Here is your all of ", "$amount ${client.GetItemName(id).lowercase()}")
            } else {
                client.showNPCChat(request.npcId, 605, arrayOf("You need 1 herb and 200 coins", "for me to grind it for you."))
            }
            return
        }

        if (!client.playerHasItem(HerbloreDefinitions.VIAL_OF_WATER_ID)) {
            client.showNPCChat(request.npcId, 605, arrayOf("You need noted vial of water for me to do that!"))
            return
        }

        val otherHerb =
            HerbloreDefinitions.herbDefinitions
                .firstOrNull { id == client.GetNotedItem(it.unfinishedPotionId) }
                ?.let { client.GetNotedItem(it.cleanId) }
                ?: -1
        val vials = client.getInvAmt(HerbloreDefinitions.VIAL_OF_WATER_ID)
        val herbs = client.getInvAmt(otherHerb)
        amount = minOf(amount, coins / HerbloreDefinitions.UNFINISHED_POTION_COST, vials, herbs)
        if (amount > 0) {
            if (herbs <= amount) {
                client.herbOptions.removeAt(request.slot)
            }
            client.deleteItem(995, amount * HerbloreDefinitions.UNFINISHED_POTION_COST)
            client.deleteItem(HerbloreDefinitions.VIAL_OF_WATER_ID, amount)
            client.deleteItem(otherHerb, amount)
            client.addItem(id, amount)
            client.checkItemUpdate()
            HerbloreNpcDialogue.showBatchResultAndContinue(client, request.npcId, "Here is your all of ", "$amount ${client.GetItemName(id).lowercase()}")
        } else {
            client.showNPCChat(
                request.npcId,
                605,
                arrayOf(
                    "You need atleast 1 herb, 1 vial of water and 1000 coins",
                    "for me to turn it into a unfinish potion.",
                ),
            )
        }
    }
}
