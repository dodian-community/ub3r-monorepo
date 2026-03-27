package net.dodian.uber.game.skills.fishing

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.skills.fishing.FishingPlugin

object FishingNpcInteractionService {
    @JvmStatic
    fun handleNpcOption(client: Client, npcId: Int, option: Int): Boolean {
        if (option != 1 && option != 2) {
            return false
        }
        if (FishingDefinitions.findSpot(npcId, option) == null) {
            return false
        }
        FishingPlugin.attempt(client, npcId, option)
        return true
    }
}
