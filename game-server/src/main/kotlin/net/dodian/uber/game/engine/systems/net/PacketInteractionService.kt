package net.dodian.uber.game.engine.systems.net

import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.objects.travel.LegendsGuildGateService
import net.dodian.uber.game.events.combat.PlayerAttackEvent
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.interaction.AttackPlayerIntent
import net.dodian.uber.game.engine.systems.interaction.ItemOnNpcIntent
import net.dodian.uber.game.engine.systems.interaction.NpcInteractionIntent
import net.dodian.uber.game.engine.systems.interaction.scheduler.InteractionTaskScheduler
import net.dodian.uber.game.engine.systems.interaction.scheduler.NpcInteractionTask
import net.dodian.uber.game.engine.systems.interaction.scheduler.PlayerInteractionTask
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry

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
            return
        }

        val npc = Server.npcManager.npcMap[npcIndex]
        if (npc == null) {
            return
        }
        if (client.randomed || client.UsingAgility) {
            return
        }
        if (client.playerPotatoState != null) {
            client.clearPlayerPotatoState()
        }
        if (option in 1..4) {
            LegendsGuildGateService.primeGuardApproach(client, npc)
        }
        if (shouldClearRedundantWalkForNpcInteraction(client, npc, option)) {
            client.resetWalkingQueue()
        }

        val intent = NpcInteractionIntent(opcode, PlayerRegistry.cycle.toLong(), npcIndex, option)
        InteractionTaskScheduler.schedule(client, intent, NpcInteractionTask(client, intent))
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
            return
        }
        if (client.randomed || client.UsingAgility) {
            return
        }

        GameEventBus.post(PlayerAttackEvent(client, npcIndex, false))
        val intent = NpcInteractionIntent(opcode, PlayerRegistry.cycle.toLong(), npcIndex, 5)
        InteractionTaskScheduler.schedule(client, intent, NpcInteractionTask(client, intent))
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
