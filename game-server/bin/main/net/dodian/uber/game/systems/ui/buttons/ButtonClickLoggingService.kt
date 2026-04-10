package net.dodian.uber.game.systems.ui.buttons

import net.dodian.uber.game.persistence.audit.ConsoleAuditLog

object ButtonClickLoggingService {
    private val ignoredUnhandledButtons = emptySet<Int>()

    @JvmStatic
    fun logClick(request: ButtonClickRequest, opcode: Int, handled: Boolean) {
        if (handled || request.rawButtonId !in ignoredUnhandledButtons) {
            ConsoleAuditLog.button(request, opcode, handled)
        }
    }
}
