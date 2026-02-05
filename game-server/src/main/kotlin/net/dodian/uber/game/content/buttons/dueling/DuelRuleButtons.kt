package net.dodian.uber.game.content.buttons.dueling

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client

object DuelRuleButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(
        53245,
        53246,
        53247,
        53248,
        53249,
        53250,
        53251,
        53252,
        53253,
        53254,
        53255,
    )

    override fun onClick(client: Client, buttonId: Int): Boolean {
        client.duelButton2(buttonId - 53245)
        return true
    }
}
