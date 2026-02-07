package net.dodian.uber.game.content.buttons.emotes

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Emotes

object BasicEmoteButtons : ButtonContent {
    override val buttonIds: IntArray = Emotes.values().map { it.buttonId }.toIntArray()

    override fun onClick(client: Client, buttonId: Int): Boolean {
        Emotes.doEmote(buttonId, client)
        return true
    }
}
