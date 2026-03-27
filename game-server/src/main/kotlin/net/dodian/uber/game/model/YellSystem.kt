package net.dodian.uber.game.model

import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.netty.listener.out.SendMessage

object YellSystem {
    @JvmStatic
    fun alertStaff(message: String) {
        if (message.contains("tradereq") || message.contains("duelreq")) {
            return
        }
        PlayerHandler.forEachActivePlayer { player ->
            if (player.position.x > 0 && player.position.y > 0 && player.playerRights >= 1) {
                player.send(SendMessage(message))
            }
        }
    }
}
