package net.dodian.uber.game.systems.net

import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.content.objects.travel.LegendsGuildGateService
import net.dodian.uber.game.events.combat.PlayerAttackEvent
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.interaction.AttackPlayerIntent
import net.dodian.uber.game.systems.interaction.ItemOnNpcIntent
import net.dodian.uber.game.systems.interaction.NpcInteractionIntent
import net.dodian.uber.game.systems.interaction.scheduler.InteractionTaskScheduler
import net.dodian.uber.game.systems.interaction.scheduler.NpcInteractionTask
import net.dodian.uber.game.systems.interaction.scheduler.PlayerInteractionTask
import net.dodian.uber.game.systems.interaction.npcs.NpcClickMetrics
import net.dodian.uber.game.systems.world.player.PlayerRegistry

/**
 * Kotlin service for player-interaction packet side-effects that must stay out
 * of Netty inbound listeners.
 */
object PacketInteractionService {
    /**
     * Processes an attack-player packet after the listener has decoded the
     * victim slot.
     */
    @JvmStatic
    fun handleAttackPlayer(client: Client, opcode: Int, victimSlot: Int) {
        if (client.deathStage >= 1) return
        PlayerRegistry.getClient(victimSlot) ?: return
        if (client.randomed || client.UsingAgility) return
        GameEventBus.post(PlayerAttackEvent(client, victimSlot, true))

        val intent = AttackPlayerIntent(opcode, PlayerRegistry.cycle.toLong(), victimSlot)
        InteractionTaskScheduler.schedule(client, intent, PlayerInteractionTask(client, intent))
    }

    /**
     * Processes a use-item-on-NPC packet after the listener has decoded the
     * packet values and validated the inventory slot/item match.
     */
    @JvmStatic
    fun handleUseItemOnNpc(client: Client, opcode: Int, itemId: Int, slot: Int, npcIndex: Int) {
        if (client.randomed || client.UsingAgility) return
        Server.npcManager.npcMap[npcIndex] ?: return

        val intent = ItemOnNpcIntent(opcode, PlayerRegistry.cycle.toLong(), itemId, slot, npcIndex)
        InteractionTaskScheduler.schedule(client, intent, NpcInteractionTask(client, intent))
    }

    /**
     * Processes an NPC click packet after the listener has decoded the target
     * index and recorded decode-time metrics.
     */
    @JvmStatic
    fun handleNpcClick(client: Client, opcode: Int, option: Int, npcIndex: Int) {
        if (npcIndex < 0) {
            NpcClickMetrics.recordRejected("invalid_index", opcode, option, npcIndex, client.playerName)
            return
        }

        val npc = Server.npcManager.npcMap[npcIndex]
        if (npc == null) {
            NpcClickMetrics.recordRejected("npc_not_found", opcode, option, npcIndex, client.playerName)
            return
        }
        if (client.randomed || client.UsingAgility) {
            NpcClickMetrics.recordRejected("blocked_state", opcode, option, npcIndex, client.playerName)
            return
        }
        if (client.playerPotato.isNotEmpty()) {
            client.playerPotato.clear()
        }
        if (option in 1..4) {
            LegendsGuildGateService.primeGuardApproach(client, npc)
        }
        if (shouldClearRedundantWalkForNpcInteraction(client, npc, option)) {
            client.resetWalkingQueue()
        }

        val intent = NpcInteractionIntent(opcode, PlayerRegistry.cycle.toLong(), npcIndex, option)
        InteractionTaskScheduler.schedule(client, intent, NpcInteractionTask(client, intent))
        NpcClickMetrics.recordScheduled(opcode, option, npc.id, npcIndex, client.playerName)
    }

    /**
     * Processes an NPC attack packet after the listener has decoded the target
     * index and recorded decode-time metrics.
     */
    @JvmStatic
    fun handleNpcAttack(client: Client, opcode: Int, npcIndex: Int) {
        PacketMagicService.clearMagicIdIfSet(client)
        if (client.deathStage >= 1) return

        val npc = Server.npcManager.npcMap[npcIndex]
        if (npc == null) {
            NpcClickMetrics.recordRejected("npc_not_found", opcode, 5, npcIndex, client.playerName)
            return
        }
        if (client.randomed || client.UsingAgility) {
            NpcClickMetrics.recordRejected("blocked_state", opcode, 5, npcIndex, client.playerName)
            return
        }

        GameEventBus.post(PlayerAttackEvent(client, npcIndex, false))
        val intent = NpcInteractionIntent(opcode, PlayerRegistry.cycle.toLong(), npcIndex, 5)
        InteractionTaskScheduler.schedule(client, intent, NpcInteractionTask(client, intent))
        NpcClickMetrics.recordScheduled(opcode, 5, npc.id, npcIndex, client.playerName)
    }

    internal fun shouldClearRedundantWalkForNpcInteraction(client: Client, npc: Npc, option: Int): Boolean {
        if (option !in 1..4) {
            return false
        }
        if (npc.position.withinDistance(client.position, 0)) {
            return false
        }
        return client.goodDistanceEntity(npc, 1)
    }
}
