package net.dodian.uber.game.runtime.action

import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.tasking.GameTask

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

    fun shouldCancel(): Boolean {
        if (!player.isActive || player.disconnected || player.deathStage > 0) {
            return true
        }
        if (policy.cancelOnCombat && player.isInCombat()) {
            return true
        }
        if (policy.cancelOnDialogue &&
            (DialogueService.hasActiveSession(player) || player.NpcDialogue != 0 || player.NpcDialogueSend || player.nextDiag > 0)
        ) {
            return true
        }
        if (policy.cancelOnMove && hasMovedSinceLastProcess(player)) {
            return true
        }
        return false
    }

    fun isActive(): Boolean = !shouldCancel()

    companion object {
        private fun hasMovedSinceLastProcess(player: Client): Boolean {
            return player.currentX != player.position.x || player.currentY != player.position.y || player.didTeleport()
        }
    }
}
