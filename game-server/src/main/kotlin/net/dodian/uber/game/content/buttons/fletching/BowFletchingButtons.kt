package net.dodian.uber.game.content.buttons.fletching

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client

object BowFletchingButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(
        34170, 34169, 34168, 34167,
        34174, 34173, 34172, 34171,
    )

    override fun onClick(client: Client, buttonId: Int): Boolean {
        val amount = when (buttonId) {
            34170, 34174 -> 1
            34169, 34173 -> 5
            34168, 34172 -> 10
            34167, 34171 -> 27
            else -> return false
        }

        val longBow = buttonId in intArrayOf(34170, 34169, 34168, 34167)
        client.fletching.fletchBow(client, longBow, amount)
        return true
    }
}

