package net.dodian.uber.game.systems.action

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.loop.GameCycleClock
import net.dodian.uber.game.engine.scheduler.QueueTaskHandle
import net.dodian.uber.game.engine.tasking.GameTaskRuntime
import net.dodian.uber.game.engine.tasking.TaskPriority
import java.util.concurrent.atomic.AtomicBoolean

object PlayerActionController {
    @JvmStatic
    fun start(
        player: Client,
        type: PlayerActionType,
        replaceReason: PlayerActionCancelReason = PlayerActionCancelReason.NEW_ACTION,
        interruptPolicy: PlayerActionInterruptPolicy = PlayerActionInterruptPolicy.DEFAULT,
        onStop: (Client, PlayerActionStopResult) -> Unit = { _, _ -> },
        block: suspend PlayerActionContext.() -> Unit,
    ): QueueTaskHandle {
        cancel(player, replaceReason)
        val startedCycle = GameCycleClock.currentCycle()
        player.activeActionCancelReason = null
        val resolved = AtomicBoolean(false)
        fun resolve(result: PlayerActionStopResult) {
            if (!resolved.compareAndSet(false, true)) {
                return
            }
            if (result is PlayerActionStopResult.Cancelled) {
                player.lastActionCancelReason = result.reason
                player.lastActionCancelCycle = GameCycleClock.currentCycle()
            }
            onStop(player, result)
            clearIfOwned(player, startedCycle)
        }
        val handle =
            GameTaskRuntime.queuePlayer(player, TaskPriority.STRONG) {
                onTerminate {
                    resolve(stopResult(player, startedCycle))
                }
                try {
                    PlayerActionContext(player, this, interruptPolicy).block()
                } finally {
                    resolve(stopResult(player, startedCycle))
                }
            }
        val queueHandle = QueueTaskHandle.from(handle)
        player.activeActionHandle = queueHandle
        player.activeActionType = type
        player.actionStartedCycle = startedCycle
        return queueHandle
    }

    @JvmStatic
    fun cancel(player: Client, reason: PlayerActionCancelReason = PlayerActionCancelReason.MANUAL_RESET) {
        if (player.activeActionHandle == null) {
            return
        }
        player.activeActionCancelReason = reason
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
        player.clearActiveActionState()
    }

    private fun clearIfOwned(player: Client, startedCycle: Long) {
        if (player.actionStartedCycle == startedCycle) {
            clear(player)
        }
    }

    private fun stopResult(player: Client, startedCycle: Long): PlayerActionStopResult {
        val cancelled = player.actionStartedCycle == startedCycle && player.activeActionCancelReason != null
        return if (cancelled) {
            PlayerActionStopResult.Cancelled(player.activeActionCancelReason!!)
        } else {
            PlayerActionStopResult.Completed
        }
    }
}
