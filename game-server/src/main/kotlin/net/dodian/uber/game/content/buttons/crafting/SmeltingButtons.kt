package net.dodian.uber.game.content.buttons.crafting

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client

object SmeltingButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(
        15147, 15146, 10247, 9110,
        15151, 15150, 15149, 15148,
        15155, 15154, 15153, 15152,
        15159, 15158, 15157, 15156,
        15163, 15162, 15161, 15160,
        29017, 29016, 24253, 16062,
        29022, 29020, 29019, 29018,
        29026, 29025, 29024, 29023,
    )

    override fun onClick(client: Client, buttonId: Int): Boolean {
        client.startSmelt(buttonId)
        return true
    }
}

