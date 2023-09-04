package net.dodian.uber.plugin

import net.dodian.server.scripting.ScriptPlugin
import net.dodian.uber.context
import net.dodian.uber.event.TypeGameEvent
import net.dodian.uber.event.TypePlayerKeyedEvent
import net.dodian.uber.game.model.entity.player.Player

fun ScriptPlugin.context() = context
val ScriptPlugin.eventBus get() = context.eventBus

inline fun <reified T : TypeGameEvent> ScriptPlugin.onEvent(
    noinline action: T.() -> Unit
) {
    eventBus.add(T::class.java) { action(it) }
}

inline fun <reified T : TypePlayerKeyedEvent> ScriptPlugin.onEvent(
    id: Number,
    noinline action: Player.(T) -> Unit
) {
    eventBus.set(id.toLong(), T::class.java, action)
}