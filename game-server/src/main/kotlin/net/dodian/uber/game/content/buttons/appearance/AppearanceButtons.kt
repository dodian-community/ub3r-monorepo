package net.dodian.uber.game.content.buttons.appearance

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.UpdateFlag
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces

object AppearanceButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(3651)

    override fun onClick(client: Client, buttonId: Int): Boolean {
        client.send(RemoveInterfaces())
        client.updateFlags.setRequired(UpdateFlag.APPEARANCE, true)
        return true
    }
}

