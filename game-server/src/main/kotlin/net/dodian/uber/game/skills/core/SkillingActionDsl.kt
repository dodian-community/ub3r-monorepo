package net.dodian.uber.game.skills.core

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.skills.core.requirements.Requirement
import net.dodian.uber.game.skills.core.requirements.RequirementBuilder
import net.dodian.uber.game.skills.core.runtime.ActionStopReason

data class ActionSpec(
    val name: String,
    val delayTicks: Int,
    val requirements: List<Requirement>,
    val animationId: Int?,
    val onStart: (Client.() -> Unit)?,
    val onCycle: (Client.() -> Unit)?,
    val onSuccess: (Client.() -> Unit)?,
    val onStop: (Client.(ActionStopReason) -> Unit)?,
)

class SkillingActionBuilder internal constructor(
    private val name: String,
) {
    private var delayTicks: Int = 1
    private var animationId: Int? = null
    private val requirements = ArrayList<Requirement>()
    private var onStart: (Client.() -> Unit)? = null
    private var onCycle: (Client.() -> Unit)? = null
    private var onSuccess: (Client.() -> Unit)? = null
    private var onStop: (Client.(ActionStopReason) -> Unit)? = null

    fun delay(ticks: Int) {
        delayTicks = ticks.coerceAtLeast(1)
    }

    fun animation(id: Int) {
        animationId = id
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
            delayTicks = delayTicks,
            requirements = requirements,
            animationId = animationId,
            onStart = onStart,
            onCycle = onCycle,
            onSuccess = onSuccess,
            onStop = onStop,
        )
    }
}

fun action(name: String, block: SkillingActionBuilder.() -> Unit): ActionSpec {
    val builder = SkillingActionBuilder(name)
    builder.block()
    return builder.build()
}
