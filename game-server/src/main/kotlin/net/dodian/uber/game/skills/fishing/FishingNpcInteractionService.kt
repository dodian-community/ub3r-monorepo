package net.dodian.uber.game.skills.fishing

import net.dodian.uber.game.model.entity.player.Client

object FishingNpcInteractionService {
    @JvmStatic
    fun handleNpcOption(client: Client, npcId: Int, option: Int): Boolean {
        return if (option == 1 || option == 2) {
            FishingService.start(client, npcId, option)
            true
        } else {
            false
        }
    }
}
