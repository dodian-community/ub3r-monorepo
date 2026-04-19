package net.dodian.uber.game.content.ui

import net.dodian.uber.game.content.skills.fletching.Fletching
import net.dodian.uber.game.content.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.content.ui.buttons.buttonBinding

object FletchingInterface : InterfaceButtonContent {
    private val longbowButtons = intArrayOf(34170, 34169, 34168, 34167)
    private val shortbowButtons = intArrayOf(34174, 34173, 34172, 34171)
    private val amountByButton = mapOf(34170 to 1, 34169 to 5, 34168 to 10, 34167 to 27, 34174 to 1, 34173 to 5, 34172 to 10, 34171 to 27)

    override val bindings =
        listOf(
            buttonBinding(-1, 0, "fletching.bows.longbow", longbowButtons) { client, request ->
                val amount = amountByButton[request.rawButtonId] ?: return@buttonBinding false
                Fletching.start(client, true, amount)
                true
            },
            buttonBinding(-1, 1, "fletching.bows.shortbow", shortbowButtons) { client, request ->
                val amount = amountByButton[request.rawButtonId] ?: return@buttonBinding false
                Fletching.start(client, false, amount)
                true
            },
        )
}
