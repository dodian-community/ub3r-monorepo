package net.dodian.uber.game.engine.systems.combat

import net.dodian.uber.game.Server
import net.dodian.uber.game.combat.attackTarget
import net.dodian.uber.game.engine.systems.follow.FollowRouting
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import org.slf4j.LoggerFactory

object CombatRuntimeService {
    private val logger = LoggerFactory.getLogger(CombatRuntimeService::class.java)
    private val combatTelemetryEnabled: Boolean by lazy { java.lang.Boolean.getBoolean("combat.telemetry.enabled") }

    @JvmStatic
    fun process(
        player: Client,
        cycleNow: Long,
    ) {
        if (!hasActiveCombat(player)) {
            return
        }
        if (player.disconnected) {
            cancel(player, CombatCancellationReason.DISCONNECTED)
            return
        }
        if (player.isLoggingOut) {
            cancel(player, CombatCancellationReason.LOGOUT)
            return
        }
        if (player.isDeathSequenceActive || player.currentHealth < 1) {
            cancel(player, CombatCancellationReason.DEATH)
            return
        }

        var engagement = player.combatEngagementState ?: run {
            CombatCommandService.syncLegacyState(player)
            return
        }
        val target = resolveTarget(player, engagement) ?: run {
            cancel(player, CombatCancellationReason.TARGET_INVALID)
            return
        }
        var cooldown = CombatStartService.restoreCooldownState(player, cycleNow)

        val policy = CombatStartService.policyFor(player, engagement.intent)
        if (!player.goodDistanceEntity(target, policy.attackDistance)) {
            if (combatTelemetryEnabled) {
                logger.info(
                    "combat.telemetry phase=target_selection player={} reason=out_of_range targetType={} targetSlot={} attackDistance={}",
                    player.playerName,
                    target.type,
                    target.slot,
                    policy.attackDistance,
                )
            }
            followTarget(player, target, engagement, cycleNow)
            return
        }

        player.resetWalkingQueue()
        if (cycleNow < cooldown.nextAttackCycle) {
            return
        }
        if (!CombatStartService.canPerformAttackTick(player)) {
            return
        }

        val attackResult = player.attackTarget()
        if (attackResult == null) {
            if (combatTelemetryEnabled) {
                logger.info(
                    "combat.telemetry phase=attack_resolution player={} reason=attack_result_null targetType={} targetSlot={} hasEngagement={} hasCooldown={}",
                    player.playerName,
                    target.type,
                    target.slot,
                    player.combatEngagementState != null,
                    player.combatCooldownState != null,
                )
            }
            return
        }

        val nextDelay = maxOf(attackResult.nextDelayTicks, 1)
        player.lastCombat = 16
        CombatCommandService.refreshAttackCadence(player, cycleNow, nextDelay, target)
        engagement = player.combatEngagementState ?: engagement
        cooldown = player.combatCooldownState ?: cooldown
        if (combatTelemetryEnabled) {
            logger.info(
                "combat.telemetry phase=attack_resolution player={} targetType={} targetSlot={} nextAttackCycle={} lastAttackCycle={} initialSwingConsumed={}",
                player.playerName,
                engagement.targetType,
                engagement.targetSlot,
                cooldown.nextAttackCycle,
                cooldown.lastAttackCycle,
                cooldown.initialSwingConsumed,
            )
        }
    }

    @JvmStatic
    fun cancel(
        player: Client,
        reason: CombatCancellationReason,
    ) {
        if (combatTelemetryEnabled) {
            logger.info(
                "combat.telemetry phase=cancel player={} reason={} targetType={} targetSlot={} hasEngagement={} hasCooldown={}",
                player.playerName,
                reason,
                player.combatEngagementState?.targetType,
                player.combatEngagementState?.targetSlot,
                player.combatEngagementState != null,
                player.combatCooldownState != null,
            )
        }
        player.resetWalkingQueue()
        CombatCommandService.resetCombatFully(player, reason)
    }

    @JvmStatic
    fun clearNpcTargets(
        npc: Npc,
        reason: CombatCancellationReason = CombatCancellationReason.TARGET_INVALID,
    ) {
        CombatHitQueueService.clearFor(npc)
        for (player in PlayerRegistry.playersOnline.values) {
            clearNpcTarget(player, npc, reason)
        }
    }

    @JvmStatic
    fun clearNpcTarget(
        player: Client,
        npc: Npc,
        reason: CombatCancellationReason = CombatCancellationReason.TARGET_INVALID,
    ) {
        val engagement = player.combatEngagementState
        val targetingNpc =
            player.target === npc ||
                (engagement != null && engagement.targetType == Entity.Type.NPC && engagement.targetSlot == npc.slot)
        if (!targetingNpc) {
            return
        }
        player.resetWalkingQueue()
        CombatCommandService.resetCombatFully(player, reason)
    }

    @JvmStatic
    fun hasActiveCombat(player: Client): Boolean = player.combatEngagementState != null

    @JvmStatic
    fun isTargetingNpc(
        player: Client,
        npc: Npc,
    ): Boolean {
        val target = player.target
        if (target === npc) {
            return true
        }
        val engagement = player.combatEngagementState ?: return false
        return engagement.targetType == Entity.Type.NPC && engagement.targetSlot == npc.slot
    }

    private fun resolveTarget(
        player: Client,
        engagement: CombatEngagementState,
    ): Entity? {
        val target =
            when (engagement.targetType) {
                Entity.Type.PLAYER -> resolveCombatTargetPlayer(engagement.targetSlot)
                Entity.Type.NPC -> Server.npcManager.getNpc(engagement.targetSlot)
            }
        if (target == null || !isValidTarget(target)) {
            return null
        }
        if (player.target !== target) {
            player.target = target
        }
        return target
    }

    private fun isValidTarget(target: Entity): Boolean =
        when (target) {
            is Client -> !target.disconnected && target.currentHealth > 0 && !target.isDeathSequenceActive
            is Npc -> target.alive && target.currentHealth > 0
            else -> false
        }

    private fun followTarget(
        player: Client,
        target: Entity,
        engagement: CombatEngagementState,
        cycleNow: Long,
    ) {
        val refreshed =
            engagement.lastFollowCycle != cycleNow &&
                (engagement.lastFollowTargetX != target.position.x ||
                    engagement.lastFollowTargetY != target.position.y ||
                    player.wQueueReadPtr == player.wQueueWritePtr)

        if (!engagement.autoFollowEnabled) {
            CombatCommandService.cancelEngagement(player, CombatCancellationReason.OUT_OF_RANGE)
            return
        }

        if (refreshed) {
            FollowRouting.routeToEntityBoundary(
                follower = player,
                targetX = target.position.x,
                targetY = target.position.y,
                targetSize = target.getSize(),
                z = player.position.z,
                preferredDestination = preferredCombatDestination(target, engagement),
                running = true,
            )
        }
        if (target is Npc) {
            player.faceNpc(target.slot)
        } else {
            player.facePlayer(target.slot)
        }
        CombatCommandService.refreshFollowState(player, target, cycleNow)
    }

    private fun preferredCombatDestination(target: Entity, engagement: CombatEngagementState): Pair<Int, Int>? {
        if (target !is Client) {
            return null
        }
        var deltaX = target.lastWalkDeltaX.coerceIn(-1, 1)
        var deltaY = target.lastWalkDeltaY.coerceIn(-1, 1)
        if (deltaX == 0 && deltaY == 0) {
            deltaX = engagement.lastFollowTargetDeltaX.coerceIn(-1, 1)
            deltaY = engagement.lastFollowTargetDeltaY.coerceIn(-1, 1)
        }
        if (deltaX == 0 && deltaY == 0) {
            return null
        }
        return (target.position.x - deltaX) to (target.position.y - deltaY)
    }
}
