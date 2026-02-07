package net.dodian.uber.game.content.buttons.dueling

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client

object DuelOfferRuleButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(26069, 26070, 26071, 30136, 2158, 26065)

    override fun onClick(client: Client, buttonId: Int): Boolean {
        client.duelButton(buttonId)
        return true
    }
}
