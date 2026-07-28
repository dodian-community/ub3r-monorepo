package net.dodian.uber.game.engine.systems.net

import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.objects.travel.LegendsGuildGateService
import net.dodian.uber.game.events.combat.PlayerAttackEvent
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.combat.AttackStartDedupeService
import net.dodian.uber.game.engine.systems.combat.AttackStartDedupeService.Decision
import net.dodian.uber.game.engine.systems.combat.CombatCommandService
import net.dodian.uber.game.engine.systems.combat.CombatIntent
import net.dodian.uber.game.engine.systems.interaction.AttackPlayerIntent
import net.dodian.uber.game.engine.systems.interaction.ItemOnNpcIntent
import net.dodian.uber.game.engine.systems.interaction.EntityInteractionReach
import net.dodian.uber.game.engine.systems.interaction.EntityReachResult
import net.dodian.uber.game.engine.systems.interaction.npcs.BankerApproachFallbackService
import net.dodian.uber.game.engine.systems.interaction.NpcInteractionIntent
import net.dodian.uber.game.engine.systems.interaction.npcs.NpcContentRegistry
import net.dodian.uber.game.engine.systems.interaction.scheduler.InteractionTaskScheduler
import net.dodian.uber.game.engine.systems.interaction.scheduler.NpcInteractionTask
import net.dodian.uber.game.engine.systems.interaction.scheduler.PlayerInteractionTask
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.engine.config.gameWorldId
import net.dodian.uber.game.persistence.audit.ConsoleAuditLog
import org.slf4j.LoggerFactory

/**
 * of Netty inbound listeners.
 */
object PacketInteractionService {
    private val logger = LoggerFactory.getLogger(PacketInteractionService::class.java)
    /**
     * victim slot.
     */
    @JvmStatic
    fun handleAttackPlayer(client: Client, opcode: Int, victimSlot: Int) {
        if (client.deathStage >= 1) return
        val victim = PlayerRegistry.getClient(victimSlot) ?: return
        if (client.randomed || client.UsingAgility) return
        when (AttackStartDedupeService.shouldAcceptAttackStart(
            player = client,
            intent = CombatIntent.ATTACK_PLAYER,
            targetType = Entity.Type.PLAYER,
            targetSlot = victimSlot,
            cycle = PlayerRegistry.cycle.toLong(),
        )) {
            Decision.ACCEPT -> {
                GameEventBus.post(PlayerAttackEvent(client, victimSlot, true))
                CombatCommandService.requestAttack(client, victim, CombatIntent.ATTACK_PLAYER)
            }
            Decision.DUPLICATE_PENDING,
            Decision.DUPLICATE_WINDOW -> return
            Decision.DUPLICATE_ACTIVE,
            Decision.DUPLICATE_COOLDOWN -> {
                refreshCombatTargetFacing(client, victim)
                return
            }
        }
    }

    /**
     * packet values and validated the inventory slot/item match.
     */
    @JvmStatic
    fun handleUseItemOnNpc(client: Client, opcode: Int, itemId: Int, slot: Int, npcIndex: Int) {
        if (client.randomed || client.UsingAgility) return
        val npc = Server.npcManager.npcMap[npcIndex] ?: return
        ConsoleAuditLog.npcClick(client, npc, 0, opcode)

        val intent = ItemOnNpcIntent(opcode, PlayerRegistry.cycle.toLong(), itemId, slot, npcIndex)
        InteractionTaskScheduler.schedule(client, intent, NpcInteractionTask(client, intent))
    }

    /**
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
        ConsoleAuditLog.npcClick(client, npc, option, opcode)
        if (client.playerPotatoState != null) {
            client.clearPlayerPotatoState()
        }
        if (option in 1..4) {
            LegendsGuildGateService.primeGuardApproach(client, npc)
            if (BankerApproachFallbackService.shouldAttemptFallback(client, npc, option)) {
                BankerApproachFallbackService.tryRouteCustomerSide(client, npc)
            }
        }
        if (shouldClearRedundantWalkForNpcInteraction(client, npc, option)) {
            client.resetWalkingQueue()
        }

        val intent = NpcInteractionIntent(opcode, PlayerRegistry.cycle.toLong(), npcIndex, option)
        InteractionTaskScheduler.schedule(client, intent, NpcInteractionTask(client, intent))
    }

    /**
     * index and recorded decode-time metrics.
     */
    @JvmStatic
    fun handleNpcAttack(client: Client, opcode: Int, npcIndex: Int) {
        PacketMagicService.clearMagicIdIfSet(client)
        if (client.deathStage >= 1) return

        val npc = Server.npcManager.npcMap[npcIndex]
        if (gameWorldId == 2) {
            logger.info("[W2-ATTACK] handleNpcAttack: player=${client.playerName}, opcode=$opcode, npcIndex=$npcIndex, npcFound=${npc != null}, npcId=${npc?.id}, npcAlive=${npc?.alive}, npcHealth=${npc?.currentHealth}")
        }
        if (npc == null) {
            return
        }
        if (client.randomed || client.UsingAgility) {
            if (gameWorldId == 2) {
                logger.info("[W2-ATTACK] handleNpcAttack rejected: randomed=${client.randomed}, UsingAgility=${client.UsingAgility}")
            }
            return
        }
        ConsoleAuditLog.npcClick(client, npc, 5, opcode)
        val dedupeDecision = AttackStartDedupeService.shouldAcceptAttackStart(
            player = client,
            intent = CombatIntent.ATTACK_NPC,
            targetType = Entity.Type.NPC,
            targetSlot = npcIndex,
            cycle = PlayerRegistry.cycle.toLong(),
        )
        if (gameWorldId == 2) {
            logger.info("[W2-ATTACK] handleNpcAttack dedupe decision: $dedupeDecision")
        }
        when (dedupeDecision) {
            Decision.ACCEPT -> {
                GameEventBus.post(PlayerAttackEvent(client, npcIndex, false))
                if (NpcContentRegistry.hasAttackHandler(npc)) {
                    if (gameWorldId == 2) {
                        logger.info("[W2-ATTACK] handleNpcAttack: NPC has attack handler, scheduling interaction task")
                    }
                    val intent = NpcInteractionIntent(opcode, PlayerRegistry.cycle.toLong(), npcIndex, 5)
                    InteractionTaskScheduler.schedule(client, intent, NpcInteractionTask(client, intent))
                } else {
                    val result = CombatCommandService.requestAttack(client, npc, CombatIntent.ATTACK_NPC)
                    if (gameWorldId == 2) {
                        logger.info("[W2-ATTACK] handleNpcAttack: requestAttack returned $result")
                    }
                }
            }
            Decision.DUPLICATE_PENDING,
            Decision.DUPLICATE_WINDOW -> return
            Decision.DUPLICATE_ACTIVE,
            Decision.DUPLICATE_COOLDOWN -> {
                refreshCombatTargetFacing(client, npc)
                return
            }
        }
    }

    internal fun shouldClearRedundantWalkForNpcInteraction(client: Client, npc: Npc, option: Int): Boolean {
        if (option !in 1..4) {
            return false
        }
        return EntityInteractionReach.resolve(client, npc, 1) == EntityReachResult.REACHED
    }

    private fun refreshCombatTargetFacing(client: Client, target: Entity) {
        client.target = target
        if (target is Npc) {
            client.faceNpc(target.slot)
        } else {
            client.facePlayer(target.slot)
        }
    }
}
