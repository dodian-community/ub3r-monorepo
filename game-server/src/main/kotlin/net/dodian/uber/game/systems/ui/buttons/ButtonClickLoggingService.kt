package net.dodian.uber.game.systems.ui.buttons

import net.dodian.utilities.buttonTraceEnabled
import org.slf4j.Logger

object ButtonClickLoggingService {
    private val ignoredUnhandledButtons = emptySet<Int>()

    @JvmStatic
    fun logClick(logger: Logger, request: ButtonClickRequest, opcode: Int, handled: Boolean) {
        if (buttonTraceEnabled && logger.isDebugEnabled) {
            logger.debug(
                "ButtonClick opcode={} buttonId={} opIndex={} activeIface={} resolvedIface={} componentId={} componentKey={} player={} handled={}",
                opcode,
                request.rawButtonId,
                request.opIndex,
                request.activeInterfaceId,
                request.interfaceId,
                request.componentId,
                request.componentKey,
                request.client.playerName,
                handled,
            )
        }
        if (!handled && request.rawButtonId !in ignoredUnhandledButtons) {
            logger.warn(
                "Unhandled button opcode={} buttonId={} opIndex={} activeIface={} resolvedIface={} componentId={} componentKey={} player={}",
                opcode,
                request.rawButtonId,
                request.opIndex,
                request.activeInterfaceId,
                request.interfaceId,
                request.componentId,
                request.componentKey,
                request.client.playerName,
            )
        }
    }
}
