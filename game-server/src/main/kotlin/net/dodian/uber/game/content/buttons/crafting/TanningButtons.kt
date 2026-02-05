package net.dodian.uber.game.content.buttons.crafting

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client

object TanningButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(
        57225, 57217, 57201, 57209,
        57229, 57221, 57205, 57213,
        57227, 57219, 57211, 57203,
        57228, 57220, 57212, 57204,
        57231, 57223, 57215, 57207,
        57232, 57224, 57216, 57208,
    )

    override fun onClick(client: Client, buttonId: Int): Boolean {
        val amount = when (buttonId) {
            57225, 57229, 57227, 57228, 57231, 57232 -> 1
            57217, 57221, 57219, 57220, 57223, 57224 -> 5
            57201, 57209, 57205, 57213, 57211, 57203,
            57212, 57204, 57215, 57207, 57216, 57208 -> 27
            else -> return false
        }

        val hideType = when (buttonId) {
            57225, 57217, 57201, 57209 -> 0
            57229, 57221, 57205, 57213 -> 1
            57227, 57219, 57211, 57203 -> 2
            57228, 57220, 57212, 57204 -> 3
            57231, 57223, 57215, 57207 -> 4
            57232, 57224, 57216, 57208 -> 5
            else -> return false
        }

        client.startTan(amount, hideType)
        return true
    }
}

