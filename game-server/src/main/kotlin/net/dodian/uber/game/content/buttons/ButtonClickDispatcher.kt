package net.dodian.uber.game.content.buttons

import net.dodian.uber.game.model.entity.player.Client


object ButtonClickDispatcher {
    @JvmStatic
    fun tryHandle(client: Client, buttonId: Int): Boolean {
        return ButtonContentRegistry.tryHandle(client, buttonId)
    }
}

