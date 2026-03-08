package net.dodian.uber.game.content.dialogue.core

import net.dodian.uber.game.model.entity.player.Client

/**
 * Compatibility wrapper. Use [DialogueRenderRegistry] for new code.
 */
object DialogueRenderCompatRegistry {
    @JvmStatic
    fun render(client: Client): Boolean = DialogueRenderRegistry.render(client)
}
