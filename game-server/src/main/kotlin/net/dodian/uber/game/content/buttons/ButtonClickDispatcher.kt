package net.dodian.uber.game.content.buttons

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.ui.buttons.InterfaceButtonService


object ButtonClickDispatcher {
    @JvmStatic
    fun tryHandle(client: Client, buttonId: Int, opIndex: Int): Boolean {
        if (InterfaceButtonService.tryHandle(client, buttonId, opIndex)) {
            return true
        }
        return ButtonContentRegistry.tryHandle(client, buttonId)
    }

    @JvmStatic
    fun tryHandle(client: Client, buttonId: Int): Boolean = tryHandle(client, buttonId, -1)
}
