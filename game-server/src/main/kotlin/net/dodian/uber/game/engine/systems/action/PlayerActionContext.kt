package net.dodian.uber.game.engine.systems.action

import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.tasking.GameTask

class PlayerActionContext internal constructor(
    val player: Client,
    private val task: GameTask,
    private val policy: PlayerActionInterruptPolicy,
) {
    suspend fun wait(ticks: Int) {
        task.wait(ticks)
    }

    suspend fun waitUntilCycle(targetCycle: Long) {
        task.waitUntilCycle(targetCycle)
    }

    fun currentCycle(): Long = task.currentCycle()

    fun cancellationReason(): PlayerActionCancelReason? {
        player.activeActionCancelReason?.let { return it }
        if (!player.isActive || (policy.cancelOnDisconnect && player.disconnected)) {
            return PlayerActionCancelReason.DISCONNECTED
        }
        if (policy.cancelOnLogout && player.isLoggingOut) {
            return PlayerActionCancelReason.LOGOUT
        }
        if (policy.cancelOnDeath && player.isDeathSequenceActive) {
            return PlayerActionCancelReason.DEATH
        }
        if (policy.cancelOnTeleport && player.doingTeleport() && player.activeActionType != PlayerActionType.TELEPORT) {
            return PlayerActionCancelReason.TELEPORT
        }
        if (policy.cancelOnCombatEntry && player.isInCombat) {
            return PlayerActionCancelReason.COMBAT_INTERRUPTED
        }
        if (policy.cancelOnDialogueOpen &&
            DialogueService.hasBlockingDialogue(player)
        ) {
            return PlayerActionCancelReason.DIALOGUE_OPENED
        }
        if (policy.cancelOnMovement && hasMovedSinceLastProcess(player)) {
            return PlayerActionCancelReason.MOVEMENT
        }
        return null
    }

    fun shouldCancel(): Boolean = cancellationReason() != null

    fun isActive(): Boolean = cancellationReason() == null

    companion object {
        private fun hasMovedSinceLastProcess(player: Client): Boolean {
            return player.currentX != player.position.x || player.currentY != player.position.y || player.didTeleport()
        }
    }
}
