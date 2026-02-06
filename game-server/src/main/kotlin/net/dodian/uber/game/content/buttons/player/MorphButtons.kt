package net.dodian.uber.game.content.buttons.player

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client

object MorphButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(23132)

    override fun onClick(client: Client, buttonId: Int): Boolean {
        if (client.morph) {
            client.unMorph()
        }
        return true
    }
}

