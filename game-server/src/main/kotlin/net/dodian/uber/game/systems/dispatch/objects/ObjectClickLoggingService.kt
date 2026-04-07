package net.dodian.uber.game.systems.dispatch.objects

import net.dodian.uber.game.systems.interaction.ObjectInteractionContext
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
