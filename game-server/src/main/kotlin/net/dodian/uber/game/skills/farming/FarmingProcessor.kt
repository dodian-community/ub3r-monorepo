package net.dodian.uber.game.skills.farming

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerHandler

class FarmingProcessor : Runnable {
    override fun run() {
        PlayerHandler.forEachActivePlayer { player ->
            val client = player as Client
            client.farming.updateFarming(client)
        }
    }
}
