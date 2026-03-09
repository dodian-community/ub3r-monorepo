package net.dodian.uber.game.content.buttons.crafting

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.skills.smithing.SmeltingInterfaceService
import net.dodian.uber.game.skills.smithing.SmithingDefinitions
import net.dodian.uber.game.model.entity.player.Client

object SmeltingButtons : ButtonContent {
    override val buttonIds: IntArray = SmithingDefinitions.smeltingButtonIds()

    override fun onClick(client: Client, buttonId: Int): Boolean {
        return SmeltingInterfaceService.startFromButton(client, buttonId)
    }
}
