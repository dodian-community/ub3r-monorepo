package net.dodian.uber.game.systems.combat

import net.dodian.uber.game.systems.combat.attackTarget
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.world.player.PlayerRegistry

object CombatRuntimeService {
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
        if (player.isDeathSequenceActive() || player.currentHealth < 1) {
            cancel(player, CombatCancellationReason.DEATH)
            return
        }

        val target = player.target ?: run {
            cancel(player, CombatCancellationReason.TARGET_INVALID)
            return
        }
        if (!isValidTarget(target)) {
            cancel(player, CombatCancellationReason.TARGET_INVALID)
            return
        }

        var state = player.combatTargetState
        if (state == null) {
            cancel(player, CombatCancellationReason.TARGET_INVALID)
            return
        }
        if (state.targetType != target.type || state.targetSlot != target.slot) {
            state =
                state.copy(
                    targetType = target.type,
                    targetSlot = target.slot,
                    startedCycle = cycleNow,
                    nextAttackCycle = cycleNow,
                    lastInRangeConfirmationCycle = cycleNow,
                    initialSwingConsumed = false,
                    lastFollowTargetX = target.position.x,
                    lastFollowTargetY = target.position.y,
                )
            player.combatTargetState = state
        }

        val policy = CombatStartService.policyFor(player, state.intent)
        if (!player.goodDistanceEntity(target, policy.attackDistance)) {
            followTarget(player, target, state, cycleNow)
            return
        }

        player.resetWalkingQueue()
        if (cycleNow < state.nextAttackCycle) {
            return
        }
        if (!CombatStartService.canPerformAttackTick(player)) {
            return
        }

        val attackResult = player.attackTarget()
        if (attackResult == null) {
            if (player.target == null || player.combatTargetState == null) {
                return
            }
            return
        }

        val nextDelay = maxOf(attackResult.nextDelayTicks, 1)
        player.combatTimer = nextDelay
        player.lastCombat = 16
        player.combatTargetState =
            (player.combatTargetState ?: state).copy(
                lastAttackCycle = cycleNow,
                nextAttackCycle = cycleNow + nextDelay,
                lastInRangeConfirmationCycle = cycleNow,
                autoFollowEnabled = true,
                lastFollowTargetX = target.position.x,
                lastFollowTargetY = target.position.y,
            )
    }

    @JvmStatic
    fun cancel(
        player: Client,
        reason: CombatCancellationReason,
    ) {
        player.combatCancellationReason = reason
        player.resetWalkingQueue()
        player.resetAttack()
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
        val state = player.combatTargetState
        val targetingNpc =
            player.target === npc ||
                (state != null && state.targetType == Entity.Type.NPC && state.targetSlot == npc.slot)
        if (!targetingNpc) {
            return
        }
        player.combatCancellationReason = reason
        player.resetWalkingQueue()
        player.faceTarget(-1)
        player.resetAttack()
    }

    @JvmStatic
    fun hasActiveCombat(player: Client): Boolean =
        player.target != null || player.combatTargetState != null

    @JvmStatic
    fun isTargetingNpc(
        player: Client,
        npc: Npc,
    ): Boolean {
        val target = player.target
        if (target === npc) {
            return true
        }
        val state = player.combatTargetState ?: return false
        return state.targetType == Entity.Type.NPC && state.targetSlot == npc.slot
    }

    private fun isValidTarget(target: Entity): Boolean =
        when (target) {
            is Client -> !target.disconnected && target.currentHealth > 0 && !target.isDeathSequenceActive()
            is Npc -> target.alive && target.currentHealth > 0
            else -> false
        }

    private fun followTarget(
        player: Client,
        target: Entity,
        state: CombatTargetState,
        cycleNow: Long,
    ) {
        val refreshed =
            state.lastFollowCycle != cycleNow &&
                (state.lastFollowTargetX != target.position.x ||
                    state.lastFollowTargetY != target.position.y ||
                    player.wQueueReadPtr == player.wQueueWritePtr)

        if (!state.autoFollowEnabled) {
            cancel(player, CombatCancellationReason.OUT_OF_RANGE)
            return
        }

        if (refreshed) {
            player.AddToRunCords(target.position.x, target.position.y, 0)
        }
        if (target is Npc) {
            player.faceNpc(target.slot)
        } else {
            player.facePlayer(target.slot)
        }
        player.combatTargetState =
            state.copy(
                lastFollowCycle = cycleNow,
                lastFollowTargetX = target.position.x,
                lastFollowTargetY = target.position.y,
            )
    }
}
