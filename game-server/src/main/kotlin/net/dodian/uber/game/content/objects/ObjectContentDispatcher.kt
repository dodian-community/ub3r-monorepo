package net.dodian.uber.game.content.objects

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.services.ObjectInteractionContext
import net.dodian.uber.game.content.objects.services.ObjectInteractionType
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

object ObjectContentDispatcher {
    private val logger = LoggerFactory.getLogger(ObjectContentDispatcher::class.java)
    private const val FALLBACK_LOG_WINDOW_MS = 60_000L
    private val lastUnhandledLogAt = ConcurrentHashMap<String, Long>()
    private val reentrancyGuard = ThreadLocal.withInitial { mutableSetOf<String>() }

    @JvmStatic
    fun tryHandleClick(
        client: Client,
        option: Int,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): Boolean {
        return tryHandle(ObjectInteractionContext.click(client, option, objectId, position, obj))
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
        return tryHandle(
            ObjectInteractionContext.useItem(
                client = client,
                objectId = objectId,
                position = position,
                obj = obj,
                itemId = itemId,
                itemSlot = itemSlot,
                interfaceId = interfaceId,
            ),
        )
    }

    @JvmStatic
    fun tryHandleMagic(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
        spellId: Int,
    ): Boolean {
        return tryHandle(ObjectInteractionContext.magic(client, objectId, position, obj, spellId))
    }

    @JvmStatic
    fun tryHandle(context: ObjectInteractionContext): Boolean {
        val key = buildReentrancyKey(context)
        val active = reentrancyGuard.get()
        if (!active.add(key)) {
            return false
        }

        try {
            val candidates = ObjectContentRegistry.resolveAll(context.objectId, context.position)
            if (candidates.isEmpty()) {
                logUnhandled(context)
                return false
            }

            for (content in candidates) {
                try {
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
                    if (handled) {
                        return true
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
            logUnhandled(context)
            return false
        } finally {
            active.remove(key)
            if (active.isEmpty()) {
                reentrancyGuard.remove()
            }
        }
    }

    private fun logUnhandled(context: ObjectInteractionContext) {
        val now = System.currentTimeMillis()
        val kind = when (context.type) {
            ObjectInteractionType.CLICK -> "click:${context.option ?: -1}"
            ObjectInteractionType.USE_ITEM -> "useItem"
            ObjectInteractionType.MAGIC -> "magic"
        }
        val key = "$kind:${context.objectId}"
        val last = lastUnhandledLogAt[key]
        if (last == null || now - last >= FALLBACK_LOG_WINDOW_MS) {
            lastUnhandledLogAt[key] = now
            logger.debug(
                "Unhandled object interaction kind={} objectId={} player={} (falling back to legacy path)",
                kind,
                context.objectId,
                context.client.playerName,
            )
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
