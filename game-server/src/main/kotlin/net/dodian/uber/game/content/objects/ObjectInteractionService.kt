package net.dodian.uber.game.content.objects

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.services.ObjectInteractionContext
import net.dodian.uber.game.content.objects.services.ObjectInteractionType
import net.dodian.uber.game.event.GameEventBus
import net.dodian.uber.game.event.events.ObjectClickEvent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.interaction.DispatchTiming
import org.slf4j.LoggerFactory

object ObjectInteractionService {
    private val logger = LoggerFactory.getLogger(ObjectInteractionService::class.java)
    private val reentrancyGuard = ThreadLocal.withInitial { mutableSetOf<String>() }

    @JvmStatic
    fun tryHandleClick(
        client: Client,
        option: Int,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): Boolean {
        return tryHandleTimed(ObjectInteractionContext.click(client, option, objectId, position, obj)).handled
    }

    @JvmStatic
    fun tryHandleUseItem(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
        itemId: Int,
        itemSlot: Int,
        interfaceId: Int,
    ): Boolean {
        return tryHandleTimed(
            ObjectInteractionContext.useItem(
                client = client,
                objectId = objectId,
                position = position,
                obj = obj,
                itemId = itemId,
                itemSlot = itemSlot,
                interfaceId = interfaceId,
            ),
        ).handled
    }

    @JvmStatic
    fun tryHandleMagic(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
        spellId: Int,
    ): Boolean {
        return tryHandleTimed(ObjectInteractionContext.magic(client, objectId, position, obj, spellId)).handled
    }

    @JvmStatic
    fun tryHandle(context: ObjectInteractionContext): Boolean {
        return tryHandleTimed(context).handled
    }

    @JvmStatic
    fun tryHandleTimed(context: ObjectInteractionContext): DispatchTiming {
        val key = buildReentrancyKey(context)
        val active = reentrancyGuard.get()
        if (!active.add(key)) {
            return DispatchTiming(false, 0L, 0L, null)
        }

        try {
            if (context.type == ObjectInteractionType.CLICK &&
                GameEventBus.postWithResult(
                    ObjectClickEvent(
                        client = context.client,
                        option = context.option ?: -1,
                        objectId = context.objectId,
                        position = context.position,
                        obj = context.obj,
                    ),
                )
            ) {
                return DispatchTiming(true, 0L, 0L, "GameEventBus")
            }

            val resolveStart = System.nanoTime()
            val candidates = ObjectContentRegistry.resolveCandidates(context.objectId, context.position)
            val resolveNs = System.nanoTime() - resolveStart
            if (candidates.isEmpty()) {
                ObjectClickLoggingService.log(logger, context, resolution = null, handled = false)
                return DispatchTiming(false, resolveNs, 0L, null)
            }

            var handlerNs = 0L
            var handlerName: String? = null
            for (resolution in candidates) {
                val content = resolution.content
                try {
                    val handlerStart = System.nanoTime()
                    val handled = when (context.type) {
                        ObjectInteractionType.CLICK -> when (context.option) {
                            1 -> content.onFirstClick(context.client, context.objectId, context.position, context.obj)
                            2 -> content.onSecondClick(context.client, context.objectId, context.position, context.obj)
                            3 -> content.onThirdClick(context.client, context.objectId, context.position, context.obj)
                            4 -> content.onFourthClick(context.client, context.objectId, context.position, context.obj)
                            5 -> content.onFifthClick(context.client, context.objectId, context.position, context.obj)
                            else -> false
                        }

                        ObjectInteractionType.USE_ITEM -> content.onUseItem(
                            client = context.client,
                            objectId = context.objectId,
                            position = context.position,
                            obj = context.obj,
                            itemId = context.itemId ?: -1,
                            itemSlot = context.itemSlot ?: -1,
                            interfaceId = context.interfaceId ?: -1,
                        )

                        ObjectInteractionType.MAGIC -> content.onMagic(
                            client = context.client,
                            objectId = context.objectId,
                            position = context.position,
                            obj = context.obj,
                            spellId = context.spellId ?: -1,
                        )
                    }
                    handlerNs += System.nanoTime() - handlerStart
                    if (handled) {
                        handlerName = content::class.java.name
                        ObjectClickLoggingService.log(logger, context, resolution = resolution, handled = true)
                        return DispatchTiming(true, resolveNs, handlerNs, handlerName)
                    }
                } catch (e: Exception) {
                    logger.error(
                        "Error handling object interaction type={} objectId={} at {} via {}",
                        context.type,
                        context.objectId,
                        context.position,
                        content::class.java.name,
                        e,
                    )
                }
            }
            ObjectClickLoggingService.log(logger, context, resolution = candidates.firstOrNull(), handled = false)
            return DispatchTiming(false, resolveNs, handlerNs, handlerName)
        } finally {
            active.remove(key)
            if (active.isEmpty()) {
                reentrancyGuard.remove()
            }
        }
    }

    private fun buildReentrancyKey(context: ObjectInteractionContext): String {
        return buildString {
            append(context.client.slot)
            append(':')
            append(context.type.name)
            append(':')
            append(context.option ?: -1)
            append(':')
            append(context.objectId)
            append(':')
            append(context.position.x)
            append(':')
            append(context.position.y)
            append(':')
            append(context.position.z)
        }
    }
}
