package net.dodian.uber.game.systems.content.npcs

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage

object NpcInteractionActionService {
    @JvmStatic
    fun openShop(client: Client, shopId: Int) {
        client.openUpShopRouted(shopId)
    }

    @JvmStatic
    fun openBank(client: Client) {
        client.openUpBankRouted()
    }

    @JvmStatic
    fun teleport(client: Client, x: Int, y: Int, z: Int, message: String?) {
        client.triggerTele(x, y, z, false)
        if (!message.isNullOrBlank()) {
            client.sendMessage(message)
        }
    }

    @JvmStatic
    fun sendMessage(client: Client, message: String) {
        if (message.isNotBlank()) {
            client.sendMessage(message)
        }
    }
}
