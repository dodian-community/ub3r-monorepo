package net.dodian.uber.game.content.dialogue.core

/**
 * Legacy NPC dialogue registration bridge.
 */
object DialogueRegistry {
    class Builder(
        private val delegate: DialogueRenderRegistry.Builder,
    ) {
        fun handle(dialogueId: Int, handler: DialogueRenderHandler) {
            delegate.handle(dialogueId, handler)
        }
    }
}
