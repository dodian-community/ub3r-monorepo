package net.dodian.uber.game.systems.action.dsl

import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.cancellation.CancellationException
import net.dodian.uber.game.content.skills.core.events.SkillActionCompleteEvent
import net.dodian.uber.game.content.skills.core.events.SkillActionInterruptEvent
import net.dodian.uber.game.content.skills.core.events.SkillActionStartEvent
import net.dodian.uber.game.content.skills.core.requirements.Requirement
import net.dodian.uber.game.content.skills.core.requirements.ValidationResult
import net.dodian.uber.game.content.skills.core.runtime.ActionStopReason
import net.dodian.uber.game.content.skills.core.runtime.ActionStopReasonMapper
import net.dodian.uber.game.event.GameEventBus
import net.dodian.uber.game.event.events.skilling.SkillingActionCycleEvent
import net.dodian.uber.game.event.events.skilling.SkillingActionStartedEvent
import net.dodian.uber.game.event.events.skilling.SkillingActionStoppedEvent
import net.dodian.uber.game.event.events.skilling.SkillingActionSucceededEvent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.persistence.DbDispatchers
import net.dodian.uber.game.persistence.repository.DbResult
import net.dodian.uber.game.systems.action.PlayerActionCancelReason
import net.dodian.uber.game.systems.action.PlayerActionContext
import net.dodian.uber.game.systems.action.PlayerActionController
import net.dodian.uber.game.systems.action.PlayerActionInterruptPolicy
import net.dodian.uber.game.systems.action.PlayerActionType

private class ActionScopeStopException(
    val reason: ActionStopReason,
) : CancellationException("Action stopped: $reason")

class ActionScope internal constructor(
    val player: Client,
    private val context: PlayerActionContext,
) {
    suspend fun waitTicks(ticks: Int) {
        if (ticks <= 0) {
            return
        }
        context.wait(ticks)
    }

    suspend fun waitUntil(
        timeoutTicks: Int? = null,
        predicate: () -> Boolean,
    ): Boolean {
        val startCycle = context.currentCycle()
        while (true) {
            if (!context.isActive()) {
                return false
            }
            if (predicate()) {
                return true
            }
            if (timeoutTicks != null && context.currentCycle() - startCycle >= timeoutTicks) {
                return false
            }
            context.wait(1)
        }
    }

    suspend fun stop(reason: ActionStopReason): Nothing {
        throw ActionScopeStopException(reason)
    }

    suspend fun ensure(
        requirement: Requirement,
        failureReason: ActionStopReason = ActionStopReason.REQUIREMENT_FAILED,
    ): Boolean {
        val result = requirement.validate(player)
        if (result is ValidationResult.Failed) {
            player.send(SendMessage(result.message))
            stop(failureReason)
        }
        return true
    }

    suspend fun ensureAll(
        requirements: Iterable<Requirement>,
        failureReason: ActionStopReason = ActionStopReason.REQUIREMENT_FAILED,
    ): Boolean {
        for (requirement in requirements) {
            ensure(requirement, failureReason)
        }
        return true
    }

    fun emitCycle(actionName: String) {
        GameEventBus.post(SkillingActionCycleEvent(player, actionName))
    }

    fun emitSuccess(actionName: String) {
        GameEventBus.post(SkillingActionSucceededEvent(player, actionName))
        GameEventBus.post(SkillActionCompleteEvent(player, actionName))
    }

    fun emitCycleSuccess(actionName: String) {
        emitCycle(actionName)
        emitSuccess(actionName)
    }

    suspend fun <T> suspendRead(
        executor: ExecutorService = DbDispatchers.accountExecutor,
        query: () -> T,
    ): DbResult<T> {
        val completed = AtomicBoolean(false)
        val result = AtomicReference<DbResult<T>>()
        executor.execute {
            result.set(
                try {
                    DbResult.Success(query())
                } catch (exception: Throwable) {
                    DbResult.Failure(exception, retryable = false)
                },
            )
            completed.set(true)
        }
        waitUntil { completed.get() }
        return result.get()
            ?: DbResult.Failure(IllegalStateException("Background DB query did not complete."), retryable = false)
    }
}

@JvmOverloads
fun playerAction(
    player: Client,
    type: PlayerActionType,
    actionName: String,
    replaceReason: PlayerActionCancelReason = PlayerActionCancelReason.NEW_ACTION,
    interruptPolicy: PlayerActionInterruptPolicy = PlayerActionInterruptPolicy.DEFAULT,
    onStart: (ActionScope.() -> Unit)? = null,
    onStop: ((Client, ActionStopReason) -> Unit)? = null,
    block: suspend ActionScope.() -> Unit,
) {
    var explicitStopReason: ActionStopReason? = null

    PlayerActionController.start(
        player = player,
        type = type,
        replaceReason = replaceReason,
        interruptPolicy = interruptPolicy,
        onStop = { stoppedPlayer, result ->
            val reason = explicitStopReason ?: ActionStopReasonMapper.fromStopResult(result)
            GameEventBus.post(SkillingActionStoppedEvent(stoppedPlayer, actionName, reason))
            GameEventBus.post(SkillActionInterruptEvent(stoppedPlayer, actionName, reason))
            onStop?.invoke(stoppedPlayer, reason)
        },
    ) {
        val scope = ActionScope(player, this)
        GameEventBus.post(SkillingActionStartedEvent(player, actionName))
        GameEventBus.post(SkillActionStartEvent(player, actionName))
        try {
            onStart?.invoke(scope)
            scope.block()
        } catch (stop: ActionScopeStopException) {
            explicitStopReason = stop.reason
        }
    }
}
