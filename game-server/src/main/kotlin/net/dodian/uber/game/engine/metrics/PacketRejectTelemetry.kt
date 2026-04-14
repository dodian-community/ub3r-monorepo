package net.dodian.uber.game.engine.metrics

object PacketRejectTelemetry {
    @JvmStatic
    fun record(opcode: Int, reason: String) {
        val safeOpcode = if (opcode in 0..255) opcode else -1
        OperationalTelemetry.incrementCounter("packet.reject.total")
        OperationalTelemetry.incrementCounter("packet.reject.opcode.$safeOpcode")
        OperationalTelemetry.incrementCounter("packet.reject.reason.$reason")
        OperationalTelemetry.incrementCounter("packet.reject.opcode.$safeOpcode.reason.$reason")
    }
}
