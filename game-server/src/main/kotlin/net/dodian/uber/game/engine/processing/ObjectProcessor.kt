package net.dodian.uber.game.engine.processing

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.model.objects.GlobalObject

class ObjectProcessor : Runnable {
    override fun run() {
        PlayerRegistry.forEachActivePlayer { player ->
            GlobalObject.updateObject(player as Client)
        }
    }
}
