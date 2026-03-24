package net.dodian.uber.game.skills.mining.api

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.skills.mining.MiningDefinitions
import net.dodian.uber.game.skills.mining.MiningService

object MiningPlugin {
    @JvmStatic
    fun attempt(client: Client, objectId: Int, position: Position): Boolean {
        val rock = MiningDefinitions.rockByObjectId[objectId] ?: return false
        return MiningService.startMining(client, rock, position)
    }

    @JvmStatic
    fun stop(client: Client, fullReset: Boolean) = MiningService.stopMining(client, fullReset)
}
