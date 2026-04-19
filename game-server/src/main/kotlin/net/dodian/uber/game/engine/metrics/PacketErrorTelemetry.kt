package net.dodian.uber.game.engine.metrics

object PacketErrorTelemetry {
    @JvmStatic
    fun recordListenerException(opcode: Int, playerName: String, slot: Int, size: Int, throwable: Throwable) {
        val safeOpcode = opcode.coerceIn(0, 255)
        val safeSlot = slot.coerceAtLeast(-1)
        val normalizedPlayer = playerName.ifBlank { "unknown" }.lowercase().replace(Regex("[^a-z0-9_]"), "_")
        OperationalTelemetry.incrementCounter("packet.listener.exception.total")
        OperationalTelemetry.incrementCounter("packet.listener.exception.opcode.$safeOpcode")
        OperationalTelemetry.incrementCounter("packet.listener.exception.slot.$safeSlot")
        OperationalTelemetry.incrementCounter("packet.listener.exception.player.$normalizedPlayer")
        OperationalTelemetry.incrementCounter("packet.listener.exception.size.${size.coerceAtLeast(0)}")
        OperationalTelemetry.incrementCounter("packet.listener.exception.type.${throwable.javaClass.simpleName.lowercase()}")
    }
}
