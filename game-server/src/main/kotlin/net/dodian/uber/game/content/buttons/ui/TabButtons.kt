package net.dodian.uber.game.content.buttons.ui

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SetTabInterface

object TabButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(83093)

    override fun onClick(client: Client, buttonId: Int): Boolean {
        client.send(SetTabInterface(21172, 3213))
        return true
    }
}

