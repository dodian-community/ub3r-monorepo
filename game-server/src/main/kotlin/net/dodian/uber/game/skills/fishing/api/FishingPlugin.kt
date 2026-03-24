package net.dodian.uber.game.skills.fishing.api

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.skills.fishing.FishingService

object FishingPlugin {
    @JvmStatic
    fun attempt(client: Client, objectId: Int, clickOption: Int) = FishingService.start(client, objectId, clickOption)
}
