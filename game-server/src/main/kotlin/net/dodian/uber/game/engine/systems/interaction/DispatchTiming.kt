package net.dodian.uber.game.engine.systems.interaction

data class DispatchTiming(
    val handled: Boolean,
    val resolveNs: Long,
    val handlerNs: Long,
    val handlerName: String?,
)
