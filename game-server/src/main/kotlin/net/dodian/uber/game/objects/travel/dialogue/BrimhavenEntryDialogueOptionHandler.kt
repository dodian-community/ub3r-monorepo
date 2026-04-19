package net.dodian.uber.game.objects.travel.dialogue

import net.dodian.uber.game.engine.systems.dialogue.DialogueIds
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.skill.agility.Agility
import net.dodian.uber.game.skill.agility.AgilityData
import kotlin.math.min

object BrimhavenEntryDialogueOptionHandler {
    @JvmStatic
    fun handle(client: Client, dialogueId: Int, button: Int): Boolean {
        if (dialogueId != DialogueIds.Misc.BRIMHAVEN_ENTRY) {
            return false
        }

        if (button == 1) {
            val amount = client.getInvAmt(536).toLong() + client.getInvAmt(537) + client.getBankAmt(536)
            var remainingBones = AgilityData.KBD_ENTRANCE_BONE_AMOUNT
            if (amount >= AgilityData.KBD_ENTRANCE_BONE_AMOUNT) {
                while (remainingBones > 0) {
                    for (slot in 0 until 28) {
                        if (remainingBones <= 0) break
                        if (client.playerItems[slot] - 1 == AgilityData.KBD_ENTRANCE_BONE_ID) {
                            client.deleteItem(AgilityData.KBD_ENTRANCE_BONE_ID, slot, 1)
                            remainingBones--
                        }
                    }
                    for (slot in 0 until 28) {
                        if (client.playerItems[slot] - 1 == AgilityData.KBD_ENTRANCE_NOTED_BONE_ID) {
                            val toDelete = min(client.playerItemsN[slot], remainingBones)
                            client.deleteItem(AgilityData.KBD_ENTRANCE_NOTED_BONE_ID, slot, toDelete)
                            remainingBones -= toDelete
                            break
                        }
                    }
                    for (slot in client.bankItems.indices) {
                        if (client.bankItems[slot] - 1 == AgilityData.KBD_ENTRANCE_BONE_ID) {
                            client.bankItemsN[slot] -= remainingBones
                            break
                        }
                    }
                    remainingBones = 0
                }
                Agility(client).kbdEntrance()
                client.checkItemUpdate()
                client.sendMessage("You sacrifice 5 dragon bones!")
            } else {
                client.sendMessage("You need to have 5 dragon bones to sacrifice!")
            }
            client.send(RemoveInterfaces())
            return true
        }

        client.send(RemoveInterfaces())
        return true
    }
}


