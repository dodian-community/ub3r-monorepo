package net.dodian.uber.game.engine.processing

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.model.`object`.GlobalObject

class ObjectProcessor : Runnable {
    override fun run() {
        PlayerHandler.forEachActivePlayer { player ->
            GlobalObject.updateObject(player as Client)
        }
    }
}
