package net.dodian.uber.game.engine.processing

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.world.player.PlayerRegistry

class FarmingProcessor : Runnable {
    override fun run() {
        PlayerRegistry.forEachActivePlayer { player ->
            val client = player as Client
            client.farming.run { client.updateFarming() }
        }
    }
}
