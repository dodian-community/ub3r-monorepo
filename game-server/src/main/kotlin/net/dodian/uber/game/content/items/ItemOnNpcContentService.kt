package net.dodian.uber.game.content.items

import net.dodian.uber.game.systems.ui.dialogue.DialogueService
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.content.Skillcape
import net.dodian.uber.game.netty.listener.out.SendMessage

object ItemOnNpcContentService {
    @JvmStatic
    fun handle(client: Client, itemId: Int, slot: Int, npcIndex: Int, npc: Npc) {
        val npcId = npc.id
        client.faceNpc(npcIndex)

        if (itemId == 5733) {
            client.playerPotato.clear()
            client.playerPotato.add(0, 2)
            client.playerPotato.add(1, npcIndex)
            client.playerPotato.add(2, npcId)
            client.playerPotato.add(3, 1)
            client.showPlayerOption(
                arrayOf(
                    "What do you wish to do?",
                    "Remove spawn",
                    "Check drops",
                    "Reload drops",
                    "Check config",
                    "Reload config!",
                ),
            )
            DialogueService.setDialogueSent(client, true)
            return
        }

        if (itemId == 4155) {
            npc.showGemConfig(client)
            return
        }

        if (npcId == 535 && (itemId == 1540 || itemId == 11286)) {
            if (itemId == 1540 && !client.playerHasItem(11286)) {
                client.showNPCChat(npcId, 596, arrayOf("You need a draconic visage!"))
                return
            }
            if (itemId == 11286 && !client.playerHasItem(1540)) {
                client.showNPCChat(npcId, 596, arrayOf("You need a anti-dragon shield!"))
                return
            }
            if (!client.playerHasItem(995, 1_500_000)) {
                client.showNPCChat(npcId, 596, arrayOf("You need 1.5 million coins!"))
                return
            }
            client.deleteItem(itemId, slot, 1)
            client.deleteItem(if (itemId == 1540) 11286 else 1540, 1)
            client.deleteItem(995, 1_500_000)
            client.addItemSlot(11284, 1, slot)
            client.checkItemUpdate()
            client.showNPCChat(npcId, 591, arrayOf("Here you go.", "Your shield is done."))
            return
        }

        if (npcId == 2794) {
            if (itemId == 1735) {
                client.addItem(1737, 1)
                client.checkItemUpdate()
            } else {
                client.send(SendMessage("You need some shears to shear this sheep!"))
            }
            return
        }

        if (npcId == 0) {
            if (client.farming.farmData.canNote(itemId, client.GetItemName(itemId).lowercase())) {
                client.showNPCChat(
                    npcId,
                    if (client.GetNotedItem(itemId) < 1) 594 else 594,
                    arrayOf(
                        if (client.GetNotedItem(itemId) < 1) {
                            "I can not note that item!"
                        } else {
                            "Here is your noted item."
                        },
                    ),
                )
                if (client.GetNotedItem(itemId) > 0) {
                    val amount = client.getInvAmt(itemId)
                    for (i in 0 until amount) {
                        client.deleteItem(itemId, 1)
                    }
                    client.addItem(client.GetNotedItem(itemId), amount)
                    client.checkItemUpdate()
                }
            } else {
                client.showNPCChat(npcId, 596, arrayOf("I can not note that type of item.", "Try use a valid farming crop."))
            }
            return
        }

        val skillcape = Skillcape.getSkillCape(itemId)
        if (skillcape != null && npcId == 6059) {
            if (client.hasSpace()) {
                client.addItem(skillcape.trimmedId + 1, 1)
                client.checkItemUpdate()
                client.showNPCChat(6059, 588, arrayOf("Here, have a skillcape hood from me."))
            } else {
                client.send(SendMessage("Not enough of space to get a skillcape hood."))
            }
            return
        }

        val gotMaxCape = client.GetItemName(itemId).contains("Max cape")
        if (gotMaxCape && npcId == 6481) {
            if (client.hasSpace()) {
                client.addItem(itemId + 1, 1)
                client.checkItemUpdate()
                client.showNPCChat(6481, 588, arrayOf("Here, have a skillcape hood from me."))
            } else {
                client.send(SendMessage("Not enough of space to get a skillcape hood."))
            }
        }
    }
}
