package net.dodian.uber.game.content.npcs.dialogue.core

import net.dodian.uber.game.model.entity.player.Client

fun interface DialogueRenderHandler {
    fun render(client: Client): Boolean
}

fun interface DialogueRenderModule {
    fun register(builder: DialogueRegistry.Builder)
}
