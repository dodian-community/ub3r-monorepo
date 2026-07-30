package net.dodian.uber.game.npc

import net.dodian.uber.game.api.content.dialogue.DialogueEmote
import net.dodian.uber.game.api.content.dialogue.DialogueOption
import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.skills.agility.AgilityTravel

internal object RugMerchant : NpcFamily by npcFamily("Rug merchant", 17, block = {
    definition {
        examine = "Proud owner of carpet co"
    }

    server {
        deathAnimation = 2304
        hitpoints = 18
    }

    options {
        talkTo(handler = ::handleRugMerchantTalkTo)
    }

    spawns {
        spawn(3287, 2814)
    }
})

internal fun handleRugMerchantTalkTo(client: Client, npc: Npc): Boolean {
    DialogueService.start(client) {
        npcChat(
            npc.id,
            DialogueEmote.DEFAULT,
            "Hello there Traveler.",
            "Do you fancy taking a carpet ride?",
            "It will cost 5000 coins.",
        )
        options(
            title = "Carpet rides cost 5k coins.",
            *rugMerchantOptions(npc.id),
            DialogueOption("Cancel") {
                playerChat(DialogueEmote.DEFAULT, "No, thank you.")
                finish()
            },
        )
    }
    return true
}

private fun rugMerchantOptions(npcId: Int): Array<DialogueOption> =
    when (npcId) {
        17 -> arrayOf(
            DialogueOption("Pollnivneach") { finishThen { chargeAndRide(it, npcId, 0) } },
            DialogueOption("Nardah") { finishThen { chargeAndRide(it, npcId, 1) } },
            DialogueOption("Bedabin Camp") { finishThen { chargeAndRide(it, npcId, 2) } },
        )
        19 -> arrayOf(
            DialogueOption("Pollnivneach") { finishThen { chargeAndRide(it, npcId, 0) } },
            DialogueOption("Nardah") { finishThen { chargeAndRide(it, npcId, 1) } },
            DialogueOption("Sophanem") { finishThen { chargeAndRide(it, npcId, 2) } },
        )
        20 -> arrayOf(
            DialogueOption("Nardah") { finishThen { chargeAndRide(it, npcId, 0) } },
            DialogueOption("Bedabin Camp") { finishThen { chargeAndRide(it, npcId, 1) } },
            DialogueOption("Sophanem") { finishThen { chargeAndRide(it, npcId, 2) } },
        )
        22 -> arrayOf(
            DialogueOption("Pollnivneach") { finishThen { chargeAndRide(it, npcId, 0) } },
            DialogueOption("Sophanem") { finishThen { chargeAndRide(it, npcId, 1) } },
            DialogueOption("Bedabin Camp") { finishThen { chargeAndRide(it, npcId, 2) } },
        )
        else -> emptyArray()
    }

private fun chargeAndRide(client: Client, npcId: Int, option: Int) {
    var missing = 5_000
    val amount = client.getInvAmt(995).toLong() + client.getBankAmt(995)
    if (amount < 5_000) {
        DialogueService.showNpcChat(
            client,
            npcId,
            594,
            arrayOf("You do not have enough coins to do this!", "You are currently missing ${missing - amount} coins."),
        )
        return
    }

    if (client.getInvAmt(995) >= missing) {
        client.deleteItem(995, missing)
    } else {
        missing -= client.getInvAmt(995)
        client.deleteItem(995, client.getInvAmt(995))
    }
    if (missing > 0) {
        client.deleteItemBank(995, missing)
    }
    client.checkItemUpdate()

    val carpet = AgilityTravel(client)
    when (npcId) {
        17 -> carpet.sophanem(option)
        19 -> carpet.bedabinCamp(option)
        20 -> carpet.pollnivneach(option)
        22 -> carpet.nardah(option)
    }
}
