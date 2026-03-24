package net.dodian.uber.game.skills.cooking

import net.dodian.uber.game.model.entity.player.Client

object CookingInputService {
    @JvmStatic
    fun startFromEnteredAmount(client: Client, amount: Int) {
        val current = client.cookingState ?: return
        CookingService.start(client, CookingRequest(current.itemId, current.cookIndex, amount))
    }
}
