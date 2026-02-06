package net.dodian.uber.game.content.buttons.ui

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces

object CloseInterfaceButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(
        83051,
        9118,
        19022,
        50001,
    )

    override fun onClick(client: Client, buttonId: Int): Boolean {
        client.send(RemoveInterfaces())
        return true
    }
}

