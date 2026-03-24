package net.dodian.uber.game.skills.cooking

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.skills.cooking.api.CookingPlugin

object CookingInputService {
    @JvmStatic
    fun startFromEnteredAmount(client: Client, amount: Int) {
        val current = client.cookingState ?: return
        CookingPlugin.start(client, CookingRequest(current.itemId, current.cookIndex, amount))
    }
}
