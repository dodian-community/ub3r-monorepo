package net.dodian.uber.game.content.buttons.magic.teleports

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client

object NormalTeleportButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf()

    override fun onClick(client: Client, buttonId: Int): Boolean = false
}
