package net.dodian.uber.game.skills.core

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.runtime.eventbus.GameEventBus
import net.dodian.uber.game.runtime.eventbus.events.skilling.SkillingActionCycleEvent
import net.dodian.uber.game.runtime.eventbus.events.skilling.SkillingActionStartedEvent
import net.dodian.uber.game.runtime.eventbus.events.skilling.SkillingActionStoppedEvent
import net.dodian.uber.game.runtime.eventbus.events.skilling.SkillingActionSucceededEvent
import net.dodian.uber.game.runtime.queue.QueueTaskHandle
import net.dodian.uber.game.runtime.task.GameTaskRuntime
import net.dodian.uber.game.runtime.task.TaskPriority

abstract class GatheringTask(
    private val actionName: String,
    private val client: Client,
    private val cycleDelayTicks: Int,
    private val requirements: List<Requirement>,
) {
    protected open val priority: TaskPriority = TaskPriority.WEAK

    fun start(
        onHandle: (QueueTaskHandle) -> Unit,
        beforeStart: () -> Unit = {},
    ): Boolean {
        val validation = validateRequirements()
        if (validation is ValidationResult.Failed) {
            client.send(SendMessage(validation.message))
            return true
        }

        beforeStart()
        onStart()
        GameEventBus.post(SkillingActionStartedEvent(client, actionName))

        val handle =
            GameTaskRuntime.queuePlayer(client, priority) {
                while (true) {
                    if (!client.isActive || client.disconnected) {
                        stop(ActionStopReason.DISCONNECTED)
                        return@queuePlayer
                    }
                    val check = validateRequirements()
                    if (check is ValidationResult.Failed) {
                        client.send(SendMessage(check.message))
                        stop(ActionStopReason.REQUIREMENT_FAILED)
                        return@queuePlayer
                    }
                    if (!onTick()) {
                        return@queuePlayer
                    }
                    wait(cycleDelayTicks)
                }
            }
        onHandle(QueueTaskHandle.from(handle))
        return true
    }

    protected fun succeedCycle() {
        GameEventBus.post(SkillingActionCycleEvent(client, actionName))
        GameEventBus.post(SkillingActionSucceededEvent(client, actionName))
    }

    protected fun stop(reason: ActionStopReason) {
        onStop(reason)
        GameEventBus.post(SkillingActionStoppedEvent(client, actionName, reason))
    }

    private fun validateRequirements(): ValidationResult {
        for (requirement in requirements) {
            val result = requirement.validate(client)
            if (result is ValidationResult.Failed) {
                return result
            }
        }
        return ValidationResult.ok()
    }

    protected abstract fun onStart()

    protected abstract fun onTick(): Boolean

    protected abstract fun onStop(reason: ActionStopReason)
}
