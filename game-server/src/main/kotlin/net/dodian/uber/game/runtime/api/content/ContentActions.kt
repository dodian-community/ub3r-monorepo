package net.dodian.uber.game.runtime.api.content

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.action.PlayerActionCancelReason
import net.dodian.uber.game.runtime.action.PlayerActionCancellationService
import net.dodian.uber.game.runtime.action.ProductionActionService
import net.dodian.uber.game.runtime.action.ProductionMode
import net.dodian.uber.game.runtime.action.ProductionRequest

typealias ContentProductionRequest = ProductionRequest
typealias ContentProductionMode = ProductionMode
typealias ContentActionCancelReason = PlayerActionCancelReason

object ContentActions {
    @JvmStatic
    fun queueProductionSelection(
        client: Client,
        request: ContentProductionRequest,
        interfaceModelZoom: Int = 190,
        titleLineBreaks: Int = 5,
    ) {
        ProductionActionService.queueSelection(client, request, interfaceModelZoom, titleLineBreaks)
    }

    @JvmStatic
    fun startPendingProduction(client: Client, cycleCount: Int): Boolean {
        return ProductionActionService.startPending(client, cycleCount)
    }

    @JvmStatic
    fun startProduction(
        client: Client,
        request: ContentProductionRequest,
        cycleCount: Int,
    ): Boolean {
        return ProductionActionService.start(client, request, cycleCount)
    }

    @JvmStatic
    @JvmOverloads
    fun cancel(
        player: Client,
        reason: ContentActionCancelReason,
        fullResetAnimation: Boolean = true,
        clearDialogue: Boolean = false,
        closeInterfaces: Boolean = false,
        resetCompatibilityState: Boolean = true,
    ) {
        PlayerActionCancellationService.cancel(
            player = player,
            reason = reason,
            fullResetAnimation = fullResetAnimation,
            clearDialogue = clearDialogue,
            closeInterfaces = closeInterfaces,
            resetCompatibilityState = resetCompatibilityState,
        )
    }
}
