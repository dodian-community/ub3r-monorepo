package net.dodian.uber.game.content.buttons.ui

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SetInterfaceWalkable

object CloseInterfaceButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(
        83051,
        9118,
        19022,
        50001,
    )

    override fun onClick(client: Client, buttonId: Int): Boolean {
        client.send(RemoveInterfaces())
        if (client.NpcDialogue == 1001) {
            client.send(SetInterfaceWalkable(-1))
        }
        if (client.NpcDialogue > 0) {
            client.NpcDialogue = 0
            client.NpcTalkTo = 0
            client.NpcDialogueSend = false
        }
        if (client.refundSlot != -1) {
            client.refundSlot = -1
        }
        if (client.herbMaking != -1) {
            client.herbMaking = -1
        }
        var refreshItems = false
        if (client.IsBanking) {
            client.IsBanking = false
            refreshItems = true
        }
        if (client.checkBankInterface) {
            client.checkBankInterface = false
            refreshItems = true
        }
        if (client.isPartyInterface) {
            client.isPartyInterface = false
            refreshItems = true
        }
        if (client.isShopping()) {
            client.MyShopID = -1
            refreshItems = true
        }
        if (refreshItems) {
            client.checkItemUpdate()
        }
        return true
    }
}
