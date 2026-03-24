package net.dodian.uber.game.skills.fletching.api

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.skills.fletching.FletchingRequest
import net.dodian.uber.game.skills.fletching.FletchingService

object FletchingPlugin {
    @JvmStatic
    fun open(client: Client, logIndex: Int) = FletchingService.openBowSelection(client, logIndex)

    @JvmStatic
    fun start(client: Client, request: FletchingRequest) = FletchingService.start(client, request)

    @JvmStatic
    fun startFromSelection(client: Client, longBow: Boolean, amount: Int) =
        FletchingService.startBowCrafting(client, longBow, amount)
}
