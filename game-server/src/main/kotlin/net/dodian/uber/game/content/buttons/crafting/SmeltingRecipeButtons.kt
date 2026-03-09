package net.dodian.uber.game.content.buttons.crafting

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.skills.smithing.SmeltingInterfaceService
import net.dodian.uber.game.skills.smithing.SmithingDefinitions
import org.slf4j.LoggerFactory

object SmeltingRecipeButtons : ButtonContent {
    private val logger = LoggerFactory.getLogger(SmeltingRecipeButtons::class.java)
    override val buttonIds: IntArray = SmithingDefinitions.frameIds()

    override val requiredInterfaceId: Int
        get() = 2400

    override fun onClick(client: Client, buttonId: Int): Boolean {
        logger.warn("Smelting recipe button click buttonId={} player={}", buttonId, client.playerName)
        return SmeltingInterfaceService.selectPendingRecipeFromFrameButton(client, buttonId)
    }
}
