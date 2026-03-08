package net.dodian.uber.game.content.buttons.dueling

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client

object DuelRuleButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(
        13813,
        13814,
        13815,
        13816,
        13817,
        13818,
        13819,
        13820,
        13821,
        13822,
        13823,
    )

    override fun onClick(client: Client, buttonId: Int): Boolean {
        val ruleIndex = buttonIds.indexOf(buttonId)
        return client.toggleDuelBodyRule(ruleIndex)
    }
}
