package net.dodian.uber.game.content.buttons.crafting

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.skills.smithing.SmeltingInterfaceService

object SmeltingQuantityButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(4000, 4001, 4002, 4003)

    override val requiredInterfaceId: Int
        get() = 2400

    override fun onClick(client: Client, buttonId: Int): Boolean =
        when (buttonId) {
            4003 -> SmeltingInterfaceService.startFromPending(client, 1)
            4002 -> SmeltingInterfaceService.startFromPending(client, 5)
            4001 -> SmeltingInterfaceService.startFromPending(client, 10)
            4000 -> SmeltingInterfaceService.promptPendingX(client)
            else -> false
        }
}
