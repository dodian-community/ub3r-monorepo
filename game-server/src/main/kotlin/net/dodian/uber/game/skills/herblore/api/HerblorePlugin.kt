package net.dodian.uber.game.skills.herblore.api

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.skills.herblore.HerbloreBatchRequest
import net.dodian.uber.game.skills.herblore.HerbloreService

object HerblorePlugin {
    @JvmStatic
    fun start(client: Client, request: HerbloreBatchRequest) = HerbloreService.processBatch(client, request)

    @JvmStatic
    fun attempt(client: Client, amount: Int): Boolean = HerbloreService.handleEnteredAmount(client, amount)
}
