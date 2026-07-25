package net.dodian.uber.game.engine.systems.interaction.npcs

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.api.content.ContentErrorPolicy
import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.npc.NpcClickEvent
import net.dodian.uber.game.api.content.ContentDispatchTiming
import net.dodian.uber.game.engine.systems.skills.SkillInteractionDispatcher
import net.dodian.uber.game.engine.systems.quests.QuestInteractionDispatcher
import net.dodian.uber.game.npc.*

object NpcContentDispatcher {
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
        if (QuestInteractionDispatcher.tryHandleNpcClick(client, option, npc)) {
            return ContentDispatchTiming(true, 0L, 0L, QuestInteractionDispatcher::class.java.name)
        }
        var resolveNs = 0L
        val resolveStart = System.nanoTime()
        val content = NpcContentRegistry.get(npc)
        resolveNs += (System.nanoTime() - resolveStart)
        if (content == null) {
            return ContentDispatchTiming(false, resolveNs, 0L, null)
        }
        val handlerStart = System.nanoTime()
        val handled = ContentErrorPolicy.runBoolean(client, "npc.dispatch.click.$option", bindingKey = "npc.click:${npc.id}:$option") {
            val opt = when (option) {
                1 -> net.dodian.uber.game.api.interaction.InteractionOption.FIRST
                2 -> net.dodian.uber.game.api.interaction.InteractionOption.SECOND
                3 -> net.dodian.uber.game.api.interaction.InteractionOption.THIRD
                4 -> net.dodian.uber.game.api.interaction.InteractionOption.FOURTH
                else -> net.dodian.uber.game.api.interaction.InteractionOption.FIRST
            }
            val ctx = net.dodian.uber.game.api.interaction.NpcInteractionContext(client, opt, npc)
            when (option) {
                1 -> content.handleFirstClick(ctx)
                2 -> content.handleSecondClick(ctx)
                3 -> content.handleThirdClick(ctx)
                4 -> content.handleFourthClick(ctx)
                else -> false
            }
        }
        val handlerNs = System.nanoTime() - handlerStart
        return ContentDispatchTiming(handled, resolveNs, handlerNs, content.name)
    }

    @JvmStatic
    fun tryHandleAttackTimed(client: Client, npc: Npc): ContentDispatchTiming {
        var resolveNs = 0L
        val resolveStart = System.nanoTime()
        val content = NpcContentRegistry.get(npc)
        resolveNs += (System.nanoTime() - resolveStart)
        if (content == null) {
            return ContentDispatchTiming(false, resolveNs, 0L, null)
        }
        val handlerStart = System.nanoTime()
        val handled = ContentErrorPolicy.runBoolean(client, "npc.dispatch.attack", bindingKey = "npc.attack:${npc.id}") {
            val ctx = net.dodian.uber.game.api.interaction.NpcInteractionContext(
                client,
                net.dodian.uber.game.api.interaction.InteractionOption.ATTACK,
                npc
            )
            content.handleAttack(ctx)
        }
        val handlerNs = System.nanoTime() - handlerStart
        return ContentDispatchTiming(handled, resolveNs, handlerNs, content.name)
    }
}
