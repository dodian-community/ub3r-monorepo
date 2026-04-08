package net.dodian.uber.game.systems.dispatch.objects

import net.dodian.uber.game.persistence.audit.ConsoleAuditLog
import net.dodian.uber.game.systems.interaction.ObjectInteractionContext

object ObjectClickLoggingService {
    private val ignoredUnhandled = emptySet<Int>()

    @JvmStatic
    fun log(
        context: ObjectInteractionContext,
        resolution: ObjectContentRegistry.ObjectResolution?,
        handled: Boolean,
        handlerSource: String? = null,
    ) {
        if (handled || context.objectId !in ignoredUnhandled) {
            ConsoleAuditLog.objectInteraction(context, resolution, handled, handlerSource)
        }
    }
}
