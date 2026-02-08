package net.dodian.uber.game.content.buttons.ui

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SetTabInterface

object TabInterfaceButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(
        83093, // Existing tab interface toggle
        27653, // Mystic: Show Equipment Stats
    )

    override fun onClick(client: Client, buttonId: Int): Boolean {
        when (buttonId) {
            83093 -> client.send(SetTabInterface(21172, 3213))
            27653 -> client.send(SetTabInterface(15106, 3213))
            else -> return false
        }
        return true
    }
}
