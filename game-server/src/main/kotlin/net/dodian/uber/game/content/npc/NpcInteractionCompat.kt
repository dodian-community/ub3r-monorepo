package net.dodian.uber.game.content.npc

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.interaction.DispatchTiming

/**
 * Compatibility wrapper. Use [NpcInteractionService] for new code.
 */
object NpcInteractionCompat {
    @JvmStatic
    fun tryHandleClick(client: Client, option: Int, npc: Npc): Boolean =
        NpcInteractionService.tryHandleClick(client, option, npc)

    @JvmStatic
    fun tryHandleAttack(client: Client, npc: Npc): Boolean =
        NpcInteractionService.tryHandleAttack(client, npc)

    @JvmStatic
    fun tryHandleClickTimed(client: Client, option: Int, npc: Npc): DispatchTiming =
        NpcInteractionService.tryHandleClickTimed(client, option, npc)

    @JvmStatic
    fun tryHandleAttackTimed(client: Client, npc: Npc): DispatchTiming =
        NpcInteractionService.tryHandleAttackTimed(client, npc)
}
