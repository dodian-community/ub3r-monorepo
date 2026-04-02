package net.dodian.uber.game.systems.api.content

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.tasking.TaskHandle
import net.dodian.uber.game.engine.tasking.NpcTaskContext
import net.dodian.uber.game.engine.tasking.PlayerTaskContext
import net.dodian.uber.game.engine.tasking.WorldTaskContext
import net.dodian.uber.game.engine.tasking.gameClock as engineGameClock
import net.dodian.uber.game.engine.tasking.npcTaskCoroutine as engineNpcTaskCoroutine
import net.dodian.uber.game.engine.tasking.playerTaskCoroutine as enginePlayerTaskCoroutine
import net.dodian.uber.game.engine.tasking.worldTaskCoroutine as engineWorldTaskCoroutine

object ContentCoroutines {
    @JvmStatic
    fun gameClock(): Long = engineGameClock()

    @JvmStatic
    fun worldTaskCoroutine(block: suspend WorldTaskContext.() -> Unit): TaskHandle = engineWorldTaskCoroutine(block = block)

    @JvmStatic
    fun playerTaskCoroutine(
        player: Client,
        block: suspend PlayerTaskContext.() -> Unit,
    ): TaskHandle = enginePlayerTaskCoroutine(player = player, block = block)

    @JvmStatic
    fun npcTaskCoroutine(
        npc: Npc,
        block: suspend NpcTaskContext.() -> Unit,
    ): TaskHandle = engineNpcTaskCoroutine(npc = npc, block = block)
}
