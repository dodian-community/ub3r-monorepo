package net.dodian.uber.game.runtime.action

sealed interface PlayerActionStopResult {
    object Completed : PlayerActionStopResult

    data class Cancelled(
        val reason: PlayerActionCancelReason,
    ) : PlayerActionStopResult
}
