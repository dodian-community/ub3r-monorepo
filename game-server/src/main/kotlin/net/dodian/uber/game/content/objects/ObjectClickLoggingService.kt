package net.dodian.uber.game.content.objects

import net.dodian.uber.game.content.objects.services.ObjectInteractionContext
import net.dodian.utilities.objectTraceEnabled
import org.slf4j.Logger

object ObjectClickLoggingService {
    private val ignoredUnhandled = emptySet<Int>()

    @JvmStatic
    fun log(
        logger: Logger,
        context: ObjectInteractionContext,
        resolution: ObjectContentRegistry.ObjectResolution?,
        handled: Boolean,
    ) {
        if (objectTraceEnabled && logger.isDebugEnabled) {
            logger.debug(
                "ObjectInteraction opcode={} type={} option={} objectId={} pos={},{},{} iface={} item={} spell={} module={} binding={} player={} handled={}",
                context.packetOpcode,
                context.type,
                context.option ?: -1,
                context.objectId,
                context.position.x,
                context.position.y,
                context.position.z,
                context.interfaceId ?: -1,
                context.itemId ?: -1,
                context.spellId ?: -1,
                resolution?.moduleName ?: "unresolved",
                resolution?.bindingKey ?: "unresolved",
                context.client.playerName,
                handled,
            )
        }
        if (!handled && context.objectId !in ignoredUnhandled) {
            logger.warn(
                "Unhandled object interaction opcode={} type={} option={} objectId={} pos={},{},{} player={}",
                context.packetOpcode,
                context.type,
                context.option ?: -1,
                context.objectId,
                context.position.x,
                context.position.y,
                context.position.z,
                context.client.playerName,
            )
        }
    }
}
