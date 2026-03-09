package net.dodian.uber.game.content.buttons.crafting

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.skills.smithing.SmeltingInterfaceService
import net.dodian.uber.game.skills.smithing.SmithingDefinitions

object SmeltingRecipeButtons : ButtonContent {
    override val buttonIds: IntArray = SmithingDefinitions.frameIds()

    override val requiredInterfaceId: Int
        get() = 2400

    override fun onClick(client: Client, buttonId: Int): Boolean {
        return SmeltingInterfaceService.selectPendingRecipeFromFrameButton(client, buttonId)
    }
}
