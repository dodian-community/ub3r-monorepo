package net.dodian.uber.game.engine.systems.net

data class PacketRegistrationReport(
    val registeredCount: Int,
    val missingCriticalOpcodes: List<Int>,
    val duplicateOverwriteCount: Int,
) {
    val hasMissingCriticalOpcodes: Boolean
        get() = missingCriticalOpcodes.isNotEmpty()

    companion object {
        @JvmField
        val CRITICAL_OPCODES = intArrayOf(2, 17, 35, 70, 72, 132, 155, 192, 228, 230, 234, 252)
    }
}
