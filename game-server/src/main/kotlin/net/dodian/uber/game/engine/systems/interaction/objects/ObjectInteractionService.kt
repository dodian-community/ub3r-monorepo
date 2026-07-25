package net.dodian.uber.game.engine.systems.interaction.objects

import net.dodian.cache.objects.GameObjectData
import net.dodian.uber.game.engine.systems.interaction.ObjectInteractionContext
import net.dodian.uber.game.engine.systems.interaction.ObjectInteractionType
import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.item.ItemOnObjectEvent
import net.dodian.uber.game.events.magic.MagicOnObjectEvent
import net.dodian.uber.game.events.objects.ObjectClickEvent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.interaction.DispatchTiming
import net.dodian.uber.game.engine.systems.skills.SkillInteractionDispatcher
import net.dodian.uber.game.engine.systems.quests.QuestInteractionDispatcher
import org.slf4j.LoggerFactory

object ObjectInteractionService {
    private val logger = LoggerFactory.getLogger(ObjectInteractionService::class.java)
    private val reentrancyGuard = ThreadLocal.withInitial { mutableSetOf<String>() }

    @JvmStatic
    fun tryHandleTimed(context: ObjectInteractionContext): DispatchTiming {
        val key = buildReentrancyKey(context)
        val active = reentrancyGuard.get()
        if (!active.add(key)) {
            if (net.dodian.uber.game.engine.config.gameWorldId == 2)
                logger.debug("[W2-DISPATCH] tryHandleTimed reentrancy objId={} option={}", context.objectId, context.option)
            return DispatchTiming(false, 0L, 0L, null)
        }

        try {
            val dispatchStartNs = System.nanoTime()
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
                ObjectClickLoggingService.log(
                    context,
                    resolution = null,
                    handled = true,
                    handlerSource = "GameEventBus",
                    elapsedNanos = System.nanoTime() - dispatchStartNs,
                )
                return DispatchTiming(true, 0L, 0L, "GameEventBus")
            }
            if (context.type == ObjectInteractionType.USE_ITEM &&
                GameEventBus.postWithResult(
                    ItemOnObjectEvent(
                        client = context.client,
                        objectId = context.objectId,
                        position = context.position,
                        obj = context.obj,
                        itemId = context.itemId ?: -1,
                        itemSlot = context.itemSlot ?: -1,
                        interfaceId = context.interfaceId ?: -1,
                    ),
                )
            ) {
                ObjectClickLoggingService.log(
                    context,
                    resolution = null,
                    handled = true,
                    handlerSource = "GameEventBus",
                    elapsedNanos = System.nanoTime() - dispatchStartNs,
                )
                return DispatchTiming(true, 0L, 0L, "GameEventBus")
            }
            if (context.type == ObjectInteractionType.MAGIC &&
                GameEventBus.postWithResult(
                    MagicOnObjectEvent(
                        client = context.client,
                        objectId = context.objectId,
                        position = context.position,
                        obj = context.obj,
                        spellId = context.spellId ?: -1,
                    ),
                )
            ) {
                ObjectClickLoggingService.log(
                    context,
                    resolution = null,
                    handled = true,
                    handlerSource = "GameEventBus",
                    elapsedNanos = System.nanoTime() - dispatchStartNs,
                )
                return DispatchTiming(true, 0L, 0L, "GameEventBus")
            }

            if (context.type == ObjectInteractionType.CLICK &&
                SkillInteractionDispatcher.tryHandleObjectClick(
                    client = context.client,
                    option = context.option ?: -1,
                    objectId = context.objectId,
                    position = context.position,
                    obj = context.obj,
                )
            ) {
                ObjectClickLoggingService.log(
                    context,
                    resolution = null,
                    handled = true,
                    handlerSource = SkillInteractionDispatcher::class.java.simpleName,
                    elapsedNanos = System.nanoTime() - dispatchStartNs,
                )
                return DispatchTiming(true, 0L, 0L, SkillInteractionDispatcher::class.java.name)
            }
            if (context.type == ObjectInteractionType.CLICK &&
                QuestInteractionDispatcher.tryHandleObjectClick(
                    client = context.client,
                    option = context.option ?: -1,
                    objectId = context.objectId,
                    position = context.position,
                    obj = context.obj,
                )
            ) {
                ObjectClickLoggingService.log(
                    context,
                    resolution = null,
                    handled = true,
                    handlerSource = QuestInteractionDispatcher::class.java.simpleName,
                    elapsedNanos = System.nanoTime() - dispatchStartNs,
                )
                return DispatchTiming(true, 0L, 0L, QuestInteractionDispatcher::class.java.name)
            }
            if (context.type == ObjectInteractionType.USE_ITEM &&
                SkillInteractionDispatcher.tryHandleItemOnObject(
                    client = context.client,
                    objectId = context.objectId,
                    position = context.position,
                    obj = context.obj,
                    itemId = context.itemId ?: -1,
                    itemSlot = context.itemSlot ?: -1,
                    interfaceId = context.interfaceId ?: -1,
                )
            ) {
                ObjectClickLoggingService.log(
                    context,
                    resolution = null,
                    handled = true,
                    handlerSource = SkillInteractionDispatcher::class.java.simpleName,
                    elapsedNanos = System.nanoTime() - dispatchStartNs,
                )
                return DispatchTiming(true, 0L, 0L, SkillInteractionDispatcher::class.java.name)
            }
            if (context.type == ObjectInteractionType.USE_ITEM &&
                QuestInteractionDispatcher.tryHandleItemOnObject(
                    client = context.client,
                    objectId = context.objectId,
                    position = context.position,
                    obj = context.obj,
                    itemId = context.itemId ?: -1,
                    itemSlot = context.itemSlot ?: -1,
                    interfaceId = context.interfaceId ?: -1,
                )
            ) {
                ObjectClickLoggingService.log(
                    context,
                    resolution = null,
                    handled = true,
                    handlerSource = QuestInteractionDispatcher::class.java.simpleName,
                    elapsedNanos = System.nanoTime() - dispatchStartNs,
                )
                return DispatchTiming(true, 0L, 0L, QuestInteractionDispatcher::class.java.name)
            }
            if (context.type == ObjectInteractionType.MAGIC &&
                SkillInteractionDispatcher.tryHandleMagicOnObject(
                    client = context.client,
                    objectId = context.objectId,
                    position = context.position,
                    obj = context.obj,
                    spellId = context.spellId ?: -1,
                )
            ) {
                ObjectClickLoggingService.log(
                    context,
                    resolution = null,
                    handled = true,
                    handlerSource = SkillInteractionDispatcher::class.java.simpleName,
                    elapsedNanos = System.nanoTime() - dispatchStartNs,
                )
                return DispatchTiming(true, 0L, 0L, SkillInteractionDispatcher::class.java.name)
            }
            if (context.type == ObjectInteractionType.MAGIC &&
                QuestInteractionDispatcher.tryHandleMagicOnObject(
                    client = context.client,
                    objectId = context.objectId,
                    position = context.position,
                    obj = context.obj,
                    spellId = context.spellId ?: -1,
                )
            ) {
                ObjectClickLoggingService.log(
                    context,
                    resolution = null,
                    handled = true,
                    handlerSource = QuestInteractionDispatcher::class.java.simpleName,
                    elapsedNanos = System.nanoTime() - dispatchStartNs,
                )
                return DispatchTiming(true, 0L, 0L, QuestInteractionDispatcher::class.java.name)
            }

            val resolveStart = System.nanoTime()
            val candidates = ObjectContentRegistry.resolveCandidates(context.objectId, context.position)
            val resolveNs = System.nanoTime() - resolveStart
            if (candidates.isEmpty()) {
                if (isReachedNoHandlerNoop(context)) {
                    ObjectClickLoggingService.logReachedNoHandler(
                        context,
                        elapsedNanos = System.nanoTime() - dispatchStartNs,
                    )
                    return DispatchTiming(true, resolveNs, 0L, CACHE_ACTION_NOOP)
                }
                ObjectClickLoggingService.log(
                    context,
                    resolution = null,
                    handled = false,
                    elapsedNanos = System.nanoTime() - dispatchStartNs,
                )
                return DispatchTiming(false, resolveNs, 0L, null)
            }

            var handlerNs = 0L
            var handlerName: String? = null
            for (resolution in candidates) {
                val content = resolution.content
                try {
                    val apiCtx = when (context.type) {
                        ObjectInteractionType.CLICK -> {
                            val opt = when (context.option) {
                                1 -> net.dodian.uber.game.api.interaction.InteractionOption.FIRST
                                2 -> net.dodian.uber.game.api.interaction.InteractionOption.SECOND
                                3 -> net.dodian.uber.game.api.interaction.InteractionOption.THIRD
                                4 -> net.dodian.uber.game.api.interaction.InteractionOption.FOURTH
                                5 -> net.dodian.uber.game.api.interaction.InteractionOption.FIFTH
                                else -> net.dodian.uber.game.api.interaction.InteractionOption.FIRST
                            }
                            net.dodian.uber.game.api.interaction.ObjectInteractionContext(
                                player = context.client,
                                option = opt,
                                objectId = context.objectId,
                                position = context.position,
                                definition = context.obj
                            )
                        }

                        ObjectInteractionType.USE_ITEM -> {
                            net.dodian.uber.game.api.interaction.ObjectInteractionContext(
                                player = context.client,
                                option = net.dodian.uber.game.api.interaction.InteractionOption.USE_ITEM,
                                objectId = context.objectId,
                                position = context.position,
                                definition = context.obj,
                                itemPayload = net.dodian.uber.game.api.interaction.ItemPayload(
                                    itemId = context.itemId ?: -1,
                                    itemSlot = context.itemSlot ?: -1,
                                    interfaceId = context.interfaceId ?: -1
                                )
                            )
                        }

                        ObjectInteractionType.MAGIC -> {
                            net.dodian.uber.game.api.interaction.ObjectInteractionContext(
                                player = context.client,
                                option = net.dodian.uber.game.api.interaction.InteractionOption.MAGIC,
                                objectId = context.objectId,
                                position = context.position,
                                definition = context.obj,
                                spellPayload = net.dodian.uber.game.api.interaction.SpellPayload(
                                    spellId = context.spellId ?: -1
                                )
                            )
                        }
                    }

                    val handlerStart = System.nanoTime()
                    val handled = when (context.type) {
                        ObjectInteractionType.CLICK -> when (context.option) {
                            1 -> content.onFirstClick(apiCtx)
                            2 -> content.onSecondClick(apiCtx)
                            3 -> content.onThirdClick(apiCtx)
                            4 -> content.onFourthClick(apiCtx)
                            5 -> content.onFifthClick(apiCtx)
                            else -> false
                        }

                        ObjectInteractionType.USE_ITEM -> content.onUseItem(apiCtx)

                        ObjectInteractionType.MAGIC -> content.onMagic(apiCtx)
                    }
                    handlerNs += System.nanoTime() - handlerStart
                    if (handled) {
                        handlerName = content::class.java.name
                        ObjectClickLoggingService.log(
                            context,
                            resolution = resolution,
                            handled = true,
                            elapsedNanos = System.nanoTime() - dispatchStartNs,
                        )
                        return DispatchTiming(true, resolveNs, handlerNs, handlerName)
                    }
                } catch (e: Throwable) {
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
            if (isReachedNoHandlerNoop(context)) {
                ObjectClickLoggingService.logReachedNoHandler(
                    context,
                    elapsedNanos = System.nanoTime() - dispatchStartNs,
                )
                return DispatchTiming(true, resolveNs, handlerNs, CACHE_ACTION_NOOP)
            }
            ObjectClickLoggingService.log(
                context,
                resolution = candidates.firstOrNull(),
                handled = false,
                elapsedNanos = System.nanoTime() - dispatchStartNs,
            )
            return DispatchTiming(false, resolveNs, handlerNs, handlerName)
        } finally {
            active.remove(key)
            if (active.isEmpty()) {
                reentrancyGuard.remove()
            }
        }
    }

    private fun isReachedNoHandlerNoop(context: ObjectInteractionContext): Boolean {
        val definition = context.obj ?: GameObjectData.forId(context.objectId)
        return ObjectClickLoggingService.isCacheActionObject(definition)
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

    private const val CACHE_ACTION_NOOP = "cache-action-noop"
}
