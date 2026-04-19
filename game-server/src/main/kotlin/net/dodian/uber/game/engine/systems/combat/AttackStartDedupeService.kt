package net.dodian.uber.game.engine.systems.combat

import net.dodian.uber.game.engine.systems.interaction.AttackPlayerIntent
import net.dodian.uber.game.engine.systems.interaction.NpcInteractionIntent
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.player.AttackStartDedupeState
import net.dodian.uber.game.model.entity.player.Client
import org.slf4j.LoggerFactory

object AttackStartDedupeService {
    private val logger = LoggerFactory.getLogger(AttackStartDedupeService::class.java)
    private val telemetryEnabled: Boolean by lazy {
        java.lang.Boolean.getBoolean("combat.dedupe.telemetry.enabled")
    }

    enum class Decision {
        ACCEPT,
        DUPLICATE_PENDING,
        DUPLICATE_ACTIVE,
        DUPLICATE_COOLDOWN,
        DUPLICATE_WINDOW,
    }

    private const val DUPLICATE_WINDOW_TICKS = 1L

    @JvmStatic
    fun shouldAcceptAttackStart(
        player: Client,
        intent: CombatIntent,
        targetType: Entity.Type,
        targetSlot: Int,
        cycle: Long,
        ignorePendingIntent: Boolean = false,
    ): Decision {
        val pendingDuplicate =
            when (val pending = player.pendingInteraction) {
                is AttackPlayerIntent -> targetType == Entity.Type.PLAYER && pending.victimIndex == targetSlot
                is NpcInteractionIntent -> targetType == Entity.Type.NPC && pending.option == 5 && pending.npcIndex == targetSlot
                else -> false
            }
        if (!ignorePendingIntent && pendingDuplicate) {
            return reject(player, Decision.DUPLICATE_PENDING, intent, targetType, targetSlot, cycle)
        }

        val lastAccepted = player.attackStartDedupeState
        if (lastAccepted != null &&
            lastAccepted.intent == intent &&
            lastAccepted.targetType == targetType &&
            lastAccepted.targetSlot == targetSlot &&
            cycle - lastAccepted.acceptedCycle <= DUPLICATE_WINDOW_TICKS
        ) {
            return reject(player, Decision.DUPLICATE_WINDOW, intent, targetType, targetSlot, cycle)
        }

        player.attackStartDedupeState = AttackStartDedupeState(intent, targetType, targetSlot, cycle)
        return Decision.ACCEPT
    }

    private fun reject(
        player: Client,
        decision: Decision,
        intent: CombatIntent,
        targetType: Entity.Type,
        targetSlot: Int,
        cycle: Long,
    ): Decision {
        if (telemetryEnabled) {
            logger.info(
                "combat.dedupe phase=reject player={} decision={} intent={} targetType={} targetSlot={} cycle={} combatTimer={} hasEngagement={} hasCooldown={}",
                player.playerName,
                decision,
                intent,
                targetType,
                targetSlot,
                cycle,
                player.combatTimer,
                player.combatEngagementState != null,
                player.combatCooldownState != null,
            )
        }
        return decision
    }
}
