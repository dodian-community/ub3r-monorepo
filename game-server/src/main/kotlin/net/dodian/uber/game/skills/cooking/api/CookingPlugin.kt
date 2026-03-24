package net.dodian.uber.game.skills.cooking.api

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.skills.cooking.CookingRequest
import net.dodian.uber.game.skills.cooking.CookingService

object CookingPlugin {
    @JvmStatic
    fun start(client: Client, request: CookingRequest) = CookingService.start(client, request)

    @JvmStatic
    fun attempt(client: Client, itemId: Int) = CookingService.start(client, itemId)
}
