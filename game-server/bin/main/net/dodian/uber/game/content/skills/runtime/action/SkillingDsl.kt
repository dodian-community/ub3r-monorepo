package net.dodian.uber.game.content.skills.runtime.action

import net.dodian.uber.game.engine.tasking.TaskPriority
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.content.skills.runtime.requirements.Requirement
import net.dodian.uber.game.content.skills.runtime.requirements.RequirementBuilder

data class ActionSpec(
    val name: String,
    val delayCalculator: (Client) -> Int,
    val requirements: List<Requirement>,
    val onStart: (Client.() -> Unit)?,
    val onCycle: (Client.() -> CycleSignal)?,
    val onSuccess: (Client.() -> Unit)?,
    val onStop: (Client.(ActionStopReason) -> Unit)?,
)

data class CycleSignal(
    val keepRunning: Boolean,
    val succeeded: Boolean = true,
    val stopReason: ActionStopReason? = null,
) {
    companion object {
        @JvmStatic
        fun success(): CycleSignal = CycleSignal(keepRunning = true, succeeded = true)

        @JvmStatic
        fun continueWithoutSuccess(): CycleSignal = CycleSignal(keepRunning = true, succeeded = false)

        @JvmStatic
        fun stop(): CycleSignal = CycleSignal(keepRunning = false, succeeded = false)

        @JvmStatic
        fun stop(reason: ActionStopReason): CycleSignal = CycleSignal(keepRunning = false, succeeded = false, stopReason = reason)
    }
}

data class RunningGatheringAction internal constructor(
    private val task: GatheringTask,
) {
    fun cancel(reason: ActionStopReason = ActionStopReason.USER_INTERRUPT) {
        task.cancel(reason)
    }
}

data class RunningProductionAction internal constructor(
    private val action: RunningGatheringAction,
) {
    fun cancel(reason: ActionStopReason = ActionStopReason.USER_INTERRUPT) {
        action.cancel(reason)
    }
}

class GatheringActionBuilder internal constructor(
    private val name: String,
) {
    private var delayCalculator: (Client) -> Int = { 1 }
    private val requirements = ArrayList<Requirement>()
    private var onStart: (Client.() -> Unit)? = null
    private var onCycle: (Client.() -> CycleSignal)? = null
    private var onSuccess: (Client.() -> Unit)? = null
    private var onStop: (Client.(ActionStopReason) -> Unit)? = null
    private var priority: TaskPriority = TaskPriority.STANDARD

    fun delay(ticks: Int) {
        delayCalculator = { ticks.coerceAtLeast(1) }
    }

    fun delay(calculator: Client.() -> Int) {
        delayCalculator = { client -> client.calculator().coerceAtLeast(1) }
    }

    fun priority(taskPriority: TaskPriority) {
        priority = taskPriority
    }

    fun requirements(block: RequirementBuilder.() -> Unit) {
        val builder = RequirementBuilder()
        builder.block()
        requirements += builder.build()
    }

    fun onStart(block: Client.() -> Unit) {
        onStart = block
    }

    fun onCycle(block: Client.() -> Unit) {
        onCycle = { block(); CycleSignal.success() }
    }

    fun onCycleWhile(block: Client.() -> Boolean) {
        onCycle = {
            if (block()) {
                CycleSignal.success()
            } else {
                CycleSignal.stop()
            }
        }
    }

    fun onCycleSignal(block: Client.() -> CycleSignal) {
        onCycle = block
    }

    fun onSuccess(block: Client.() -> Unit) {
        onSuccess = block
    }

    fun onStop(block: Client.(ActionStopReason) -> Unit) {
        onStop = block
    }

    fun build(): ActionSpec {
        return ActionSpec(
            name = name,
            delayCalculator = delayCalculator,
            requirements = requirements,
            onStart = onStart,
            onCycle = onCycle,
            onSuccess = onSuccess,
            onStop = onStop,
        )
    }

    fun start(
        client: Client,
        beforeStart: () -> Unit = {},
    ): RunningGatheringAction? {
        val spec = build()
        val task =
            object : GatheringTask(
                actionName = spec.name,
                client = client,
                delayCalculator = spec.delayCalculator,
                requirements = spec.requirements,
                priority = priority,
            ) {
                override fun onStart() {
                    spec.onStart?.invoke(client)
                }

                override fun onTick(): Boolean {
                    val signal = spec.onCycle?.invoke(client) ?: CycleSignal.success()
                    if (!signal.keepRunning) {
                        signal.stopReason?.let { cancel(it) }
                        return false
                    }
                    if (signal.succeeded) {
                        spec.onSuccess?.invoke(client)
                        succeedCycle()
                    }
                    return true
                }

                override fun onStop(reason: ActionStopReason) {
                    spec.onStop?.invoke(client, reason)
                }
            }
        return if (task.start(beforeStart)) {
            RunningGatheringAction(task)
        } else {
            null
        }
    }
}

fun gatheringAction(name: String, block: GatheringActionBuilder.() -> Unit): GatheringActionBuilder {
    val builder = GatheringActionBuilder(name)
    builder.block()
    return builder
}

class ProductionActionBuilder internal constructor(
    name: String,
) {
    private val gathering = GatheringActionBuilder(name)

    fun delay(ticks: Int) {
        gathering.delay(ticks)
    }

    fun delay(calculator: Client.() -> Int) {
        gathering.delay(calculator)
    }

    fun priority(taskPriority: TaskPriority) {
        gathering.priority(taskPriority)
    }

    fun requirements(block: RequirementBuilder.() -> Unit) {
        gathering.requirements(block)
    }

    fun onStart(block: Client.() -> Unit) {
        gathering.onStart(block)
    }

    fun onCycle(block: Client.() -> Unit) {
        gathering.onCycle(block)
    }

    fun onCycleWhile(block: Client.() -> Boolean) {
        gathering.onCycleWhile(block)
    }

    fun onCycleSignal(block: Client.() -> CycleSignal) {
        gathering.onCycleSignal(block)
    }

    fun onSuccess(block: Client.() -> Unit) {
        gathering.onSuccess(block)
    }

    fun onStop(block: Client.(ActionStopReason) -> Unit) {
        gathering.onStop(block)
    }

    fun start(client: Client, beforeStart: () -> Unit = {}): RunningProductionAction? {
        val running = gathering.start(client, beforeStart) ?: return null
        return RunningProductionAction(running)
    }
}

fun productionAction(name: String, block: ProductionActionBuilder.() -> Unit): ProductionActionBuilder {
    val builder = ProductionActionBuilder(name)
    builder.block()
    return builder
}

fun action(name: String, block: GatheringActionBuilder.() -> Unit): ActionSpec {
    val builder = GatheringActionBuilder(name)
    builder.block()
    return builder.build()
}
