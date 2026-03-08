package net.dodian.uber.game.content.buttons.dueling

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client

object DuelOfferRuleButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(6698, 6699, 6697, 6702)

    override fun onClick(client: Client, buttonId: Int): Boolean {
        val ruleIndex = when (buttonId) {
            6698 -> 0
            6699 -> 1
            6697 -> 2
            6702 -> 7
            else -> return false
        }
        return client.toggleDuelRule(ruleIndex)
    }
}
