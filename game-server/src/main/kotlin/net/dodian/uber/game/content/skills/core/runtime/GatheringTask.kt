package net.dodian.uber.game.content.skills.core.runtime

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.event.GameEventBus
import net.dodian.uber.game.event.events.skilling.SkillingActionCycleEvent
import net.dodian.uber.game.event.events.skilling.SkillingActionStartedEvent
import net.dodian.uber.game.event.events.skilling.SkillingActionStoppedEvent
import net.dodian.uber.game.event.events.skilling.SkillingActionSucceededEvent
import net.dodian.uber.game.content.skills.core.events.SkillActionCompleteEvent
import net.dodian.uber.game.content.skills.core.events.SkillActionInterruptEvent
import net.dodian.uber.game.content.skills.core.events.SkillActionStartEvent
import net.dodian.uber.game.content.skills.core.requirements.Requirement
import net.dodian.uber.game.content.skills.core.requirements.ValidationResult
import net.dodian.uber.game.engine.tasking.GameTaskRuntime
import net.dodian.uber.game.engine.tasking.TaskHandle
import net.dodian.uber.game.engine.tasking.TaskPriority

abstract class GatheringTask(
    private val actionName: String,
    private val client: Client,
    private val delayCalculator: (Client) -> Int,
    private val requirements: List<Requirement>,
    private val priority: TaskPriority = TaskPriority.STANDARD,
) {
    @Volatile
    private var handle: TaskHandle? = null
    @Volatile
    private var stopped = false

    fun start(
        beforeStart: () -> Unit = {},
    ): Boolean {
        val validation = validateRequirements()
        if (validation is ValidationResult.Failed) {
            client.sendMessage(validation.message)
            return false
        }

        beforeStart()
        onStart()
        GameEventBus.post(SkillingActionStartedEvent(client, actionName))
        GameEventBus.post(SkillActionStartEvent(client, actionName))

        handle =
            GameTaskRuntime.queuePlayer(client, priority) {
                while (true) {
                    if (stopped) {
                        return@queuePlayer
                    }
                    if (!client.isActive || client.disconnected) {
                        stop(ActionStopReason.DISCONNECTED)
                        return@queuePlayer
                    }
                    val check = validateRequirements()
                    if (check is ValidationResult.Failed) {
                        client.sendMessage(check.message)
                        stop(ActionStopReason.REQUIREMENT_FAILED)
                        return@queuePlayer
                    }
                    if (!onTick()) {
                        if (!stopped) {
                            stop(ActionStopReason.COMPLETED)
                        }
                        return@queuePlayer
                    }
                    wait(delayCalculator(client).coerceAtLeast(1))
                }
            }
        return true
    }

    protected fun succeedCycle() {
        GameEventBus.post(SkillingActionCycleEvent(client, actionName))
        GameEventBus.post(SkillingActionSucceededEvent(client, actionName))
        GameEventBus.post(SkillActionCompleteEvent(client, actionName))
    }

    fun cancel(reason: ActionStopReason = ActionStopReason.USER_INTERRUPT) {
        stop(reason)
    }

    protected fun stop(reason: ActionStopReason) {
        if (stopped) {
            return
        }
        stopped = true
        onStop(reason)
        GameEventBus.post(SkillingActionStoppedEvent(client, actionName, reason))
        GameEventBus.post(SkillActionInterruptEvent(client, actionName, reason))
        handle?.cancel()
        handle = null
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
