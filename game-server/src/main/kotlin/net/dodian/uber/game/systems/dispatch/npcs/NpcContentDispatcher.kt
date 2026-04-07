package net.dodian.uber.game.systems.dispatch.npcs

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.npc.NpcClickEvent
import net.dodian.uber.game.systems.api.content.ContentDispatchTiming
import net.dodian.uber.game.systems.skills.SkillInteractionDispatcher
import org.slf4j.LoggerFactory

object NpcContentDispatcher {
    private val logger = LoggerFactory.getLogger(NpcContentDispatcher::class.java)

    @JvmStatic
    fun tryHandleClick(client: Client, option: Int, npc: Npc): Boolean {
        return tryHandleClickTimed(client, option, npc).handled
    }

    @JvmStatic
    fun tryHandleAttack(client: Client, npc: Npc): Boolean {
        return tryHandleAttackTimed(client, npc).handled
    }

    @JvmStatic
    fun tryHandleClickTimed(client: Client, option: Int, npc: Npc): ContentDispatchTiming {
        if (GameEventBus.postWithResult(NpcClickEvent(client, option, npc))) {
            return ContentDispatchTiming(true, 0L, 0L, "GameEventBus")
        }
        if (SkillInteractionDispatcher.tryHandleNpcClick(client, option, npc)) {
            return ContentDispatchTiming(true, 0L, 0L, SkillInteractionDispatcher::class.java.name)
        }
        var resolveNs = 0L
        val resolveStart = System.nanoTime()
        val content = NpcContentRegistry.get(npc.id)
        resolveNs += (System.nanoTime() - resolveStart)
        if (content == null) {
            return ContentDispatchTiming(false, resolveNs, 0L, null)
        }
        val handlerStart = System.nanoTime()
        val handled =
            try {
                when (option) {
                    1 -> content.onFirstClick(client, npc)
                    2 -> content.onSecondClick(client, npc)
                    3 -> content.onThirdClick(client, npc)
                    4 -> content.onFourthClick(client, npc)
                    else -> false
                }
            } catch (e: RuntimeException) {
                logger.error(
                    "Error handling npc click (option={}, npcId={}) via {}",
                    option,
                    npc.id,
                    content.name,
                    e,
                )
                false
            }
        val handlerNs = System.nanoTime() - handlerStart
        return ContentDispatchTiming(handled, resolveNs, handlerNs, content.name)
    }

    @JvmStatic
    fun tryHandleAttackTimed(client: Client, npc: Npc): ContentDispatchTiming {
        var resolveNs = 0L
        val resolveStart = System.nanoTime()
        val content = NpcContentRegistry.get(npc.id)
        resolveNs += (System.nanoTime() - resolveStart)
        if (content == null) {
            return ContentDispatchTiming(false, resolveNs, 0L, null)
        }
        val handlerStart = System.nanoTime()
        val handled =
            try {
                content.onAttack(client, npc)
            } catch (e: RuntimeException) {
                logger.error(
                    "Error handling npc attack (npcId={}) via {}",
                    npc.id,
                    content.name,
                    e,
                )
                false
            }
        val handlerNs = System.nanoTime() - handlerStart
        return ContentDispatchTiming(handled, resolveNs, handlerNs, content.name)
    }
}
