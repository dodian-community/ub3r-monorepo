package net.dodian.uber.game.runtime.action

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.loop.GameCycleClock
import net.dodian.uber.game.runtime.scheduler.QueueTaskHandle
import net.dodian.uber.game.runtime.tasking.GameTaskRuntime
import net.dodian.uber.game.runtime.tasking.TaskPriority

object PlayerActionController {
    @JvmStatic
    fun start(
        player: Client,
        type: PlayerActionType,
        interruptPolicy: PlayerActionInterruptPolicy = PlayerActionInterruptPolicy.DEFAULT,
        onStop: (Client) -> Unit = {},
        block: suspend PlayerActionContext.() -> Unit,
    ): QueueTaskHandle {
        cancel(player)
        val startedCycle = GameCycleClock.currentCycle()
        val handle =
            GameTaskRuntime.queuePlayer(player, TaskPriority.STRONG) {
                onTerminate {
                    onStop(player)
                    clearIfOwned(player, startedCycle)
                }
                try {
                    PlayerActionContext(player, this, interruptPolicy).block()
                } finally {
                    onStop(player)
                    clearIfOwned(player, startedCycle)
                }
            }
        val queueHandle = QueueTaskHandle.from(handle)
        player.activeActionHandle = queueHandle
        player.activeActionType = type
        player.actionStartedCycle = startedCycle
        return queueHandle
    }

    @JvmStatic
    fun cancel(player: Client) {
        player.cancelActiveAction()
    }

    @JvmStatic
    fun isActive(player: Client, type: PlayerActionType? = null): Boolean {
        val handle = player.activeActionHandle ?: return false
        if (handle.isCancelled() || handle.isCompleted()) {
            clear(player)
            return false
        }
        return type == null || player.activeActionType == type
    }

    @JvmStatic
    fun clear(player: Client) {
        player.activeActionHandle = null
        player.activeActionType = null
        player.actionStartedCycle = 0L
    }

    private fun clearIfOwned(player: Client, startedCycle: Long) {
        if (player.actionStartedCycle == startedCycle) {
            clear(player)
        }
    }
}
