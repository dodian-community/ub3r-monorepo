package net.dodian.uber.game.systems.ui.buttons

import net.dodian.uber.game.model.entity.player.Client

object InterfaceButtonService {
    @JvmStatic
    fun tryHandle(client: Client, rawButtonId: Int, opIndex: Int): Boolean {
        return InterfaceButtonRegistry.tryHandle(client, rawButtonId, opIndex)
    }
}

