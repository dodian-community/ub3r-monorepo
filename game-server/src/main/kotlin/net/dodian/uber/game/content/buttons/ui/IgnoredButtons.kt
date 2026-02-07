package net.dodian.uber.game.content.buttons.ui

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client

object IgnoredButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(
        26076,
        4130,
        130,
        3014, 3016, 3017,
    )

    override fun onClick(client: Client, buttonId: Int): Boolean = true
}
