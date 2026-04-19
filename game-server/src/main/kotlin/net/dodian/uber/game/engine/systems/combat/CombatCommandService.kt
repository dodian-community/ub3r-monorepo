package net.dodian.uber.game.engine.systems.combat

import net.dodian.uber.game.combat.getAttackStyle
import net.dodian.uber.game.engine.loop.GameCycleClock
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

object CombatCommandService {
    enum class AttackRequestResult {
        STARTED,
        STARTED_DURING_COOLDOWN,
        REFRESHED_SAME_TARGET,
        RETARGETED,
        REJECTED_INVALID,
    }

    @JvmStatic
    fun requestAttack(
        player: Client,
        target: Entity,
        intent: CombatIntent,
    ): AttackRequestResult {
        if (!isValidTarget(target)) {
            return AttackRequestResult.REJECTED_INVALID
        }

        val cycle = GameCycleClock.currentCycle()
        val currentEngagement = player.combatEngagementState
        val cooldown = normalizeCooldownState(player, cycle)
        player.clearCombatCancellationReason()

        if (matches(currentEngagement, target, intent)) {
            applyFacing(player, target)
            player.combatEngagementState = refreshEngagement(currentEngagement!!, target, cycle)
            syncLegacyState(player)
            return AttackRequestResult.REFRESHED_SAME_TARGET
        }

        player.resetWalkingQueue()
        applyFacing(player, target)

        val nextCooldown =
            cooldown ?: CombatCooldownState(
                attackStyleAtStart = player.getAttackStyle(),
                initialSwingConsumed = false,
                nextAttackCycle = cycle,
                lastAttackCycle = 0L,
            )

        player.combatEngagementState = createEngagement(target, intent, cycle)
        player.combatCooldownState = nextCooldown
        syncLegacyState(player)

        return when {
            currentEngagement != null -> AttackRequestResult.RETARGETED
            nextCooldown.nextAttackCycle > cycle || nextCooldown.initialSwingConsumed -> AttackRequestResult.STARTED_DURING_COOLDOWN
            else -> AttackRequestResult.STARTED
        }
    }

    @JvmStatic
    fun cancelEngagement(
        player: Client,
        reason: CombatCancellationReason,
    ) {
        player.combatCancellationReason = reason
        player.target = null
        player.faceTarget(-1)
        player.clearCombatEngagementState()
        player.clearCombatTargetState()
        player.clearAttackStartDedupeState()
    }

    @JvmStatic
    fun resetCombatFully(
        player: Client,
        reason: CombatCancellationReason,
    ) {
        cancelEngagement(player, reason)
        player.clearCombatCooldownState()
        player.combatTimer = 0
    }

    @JvmStatic
    fun markInitialSwingConsumed(player: Client): Boolean {
        val cooldown = player.combatCooldownState ?: return true
        if (cooldown.initialSwingConsumed) {
            return true
        }
        val engagement = player.combatEngagementState ?: return false
        val cycle = GameCycleClock.currentCycle()
        if (cycle < engagement.startedCycle) {
            return false
        }
        player.combatCooldownState =
            cooldown.copy(
                initialSwingConsumed = true,
            )
        player.combatEngagementState =
            engagement.copy(
                lastInRangeConfirmationCycle = cycle,
            )
        syncLegacyState(player)
        return true
    }

    @JvmStatic
    fun refreshAttackCadence(
        player: Client,
        cycleNow: Long,
        nextDelay: Int,
        target: Entity,
    ) {
        val cooldown =
            normalizeCooldownState(player, cycleNow)
                ?: CombatCooldownState(
                    attackStyleAtStart = player.getAttackStyle(),
                    initialSwingConsumed = true,
                    nextAttackCycle = cycleNow + maxOf(nextDelay, 1),
                    lastAttackCycle = cycleNow,
                )
        val engagement =
            player.combatEngagementState ?: createEngagement(target, inferIntent(target), cycleNow)

        player.combatCooldownState =
            cooldown.copy(
                initialSwingConsumed = true,
                lastAttackCycle = cycleNow,
                nextAttackCycle = cycleNow + maxOf(nextDelay, 1),
            )
        player.combatEngagementState =
            engagement.copy(
                lastInRangeConfirmationCycle = cycleNow,
                lastFollowCycle = cycleNow,
                lastFollowTargetX = target.position.x,
                lastFollowTargetY = target.position.y,
                lastFollowTargetDeltaX = resolveTargetDeltaX(target),
                lastFollowTargetDeltaY = resolveTargetDeltaY(target),
            )
        player.combatTimer = maxOf(nextDelay, 1)
        syncLegacyState(player)
    }

    @JvmStatic
    fun refreshFollowState(
        player: Client,
        target: Entity,
        cycleNow: Long,
    ) {
        val engagement = player.combatEngagementState ?: return
        player.combatEngagementState = refreshEngagement(engagement, target, cycleNow)
        syncLegacyState(player)
    }

    @JvmStatic
    fun ensureCooldownState(
        player: Client,
        cycleNow: Long,
    ): CombatCooldownState {
        val existing = normalizeCooldownState(player, cycleNow)
        if (existing != null) {
            player.combatCooldownState = existing
            syncLegacyState(player)
            return existing
        }
        val created =
            CombatCooldownState(
                attackStyleAtStart = player.getAttackStyle(),
                initialSwingConsumed = false,
                nextAttackCycle = cycleNow,
                lastAttackCycle = 0L,
            )
        player.combatCooldownState = created
        syncLegacyState(player)
        return created
    }

    @JvmStatic
    fun syncLegacyState(player: Client) {
        val engagement = player.combatEngagementState
        val cooldown = player.combatCooldownState
        if (engagement == null || cooldown == null) {
            player.clearCombatTargetState()
            return
        }
        player.combatTargetState =
            CombatTargetState(
                intent = engagement.intent,
                targetType = engagement.targetType,
                targetSlot = engagement.targetSlot,
                startedCycle = engagement.startedCycle,
                lastInRangeConfirmationCycle = engagement.lastInRangeConfirmationCycle,
                attackStyleAtStart = cooldown.attackStyleAtStart,
                initialSwingConsumed = cooldown.initialSwingConsumed,
                nextAttackCycle = cooldown.nextAttackCycle,
                lastAttackCycle = cooldown.lastAttackCycle,
                autoFollowEnabled = engagement.autoFollowEnabled,
                lastFollowCycle = engagement.lastFollowCycle,
                lastFollowTargetX = engagement.lastFollowTargetX,
                lastFollowTargetY = engagement.lastFollowTargetY,
                lastFollowTargetDeltaX = engagement.lastFollowTargetDeltaX,
                lastFollowTargetDeltaY = engagement.lastFollowTargetDeltaY,
            )
    }

    @JvmStatic
    fun normalizeCooldownState(
        player: Client,
        cycleNow: Long,
    ): CombatCooldownState? {
        player.combatCooldownState?.let { return it }
        player.combatTargetState?.let { legacy ->
            return CombatCooldownState(
                attackStyleAtStart = legacy.attackStyleAtStart,
                initialSwingConsumed = legacy.initialSwingConsumed,
                nextAttackCycle = legacy.nextAttackCycle,
                lastAttackCycle = legacy.lastAttackCycle,
            )
        }
        if (player.combatTimer > 0) {
            return CombatCooldownState(
                attackStyleAtStart = player.getAttackStyle(),
                initialSwingConsumed = true,
                nextAttackCycle = cycleNow + player.combatTimer.toLong(),
                lastAttackCycle = (cycleNow - 1L).coerceAtLeast(0L),
            )
        }
        return null
    }

    private fun createEngagement(
        target: Entity,
        intent: CombatIntent,
        cycle: Long,
    ): CombatEngagementState =
        CombatEngagementState(
            intent = intent,
            targetType = target.type,
            targetSlot = target.slot,
            startedCycle = cycle,
            lastInRangeConfirmationCycle = cycle,
            autoFollowEnabled = true,
            lastFollowCycle = cycle,
            lastFollowTargetX = target.position.x,
            lastFollowTargetY = target.position.y,
            lastFollowTargetDeltaX = resolveTargetDeltaX(target),
            lastFollowTargetDeltaY = resolveTargetDeltaY(target),
        )

    private fun refreshEngagement(
        engagement: CombatEngagementState,
        target: Entity,
        cycle: Long,
    ): CombatEngagementState =
        engagement.copy(
            lastInRangeConfirmationCycle = cycle,
            autoFollowEnabled = true,
            lastFollowCycle = cycle,
            lastFollowTargetX = target.position.x,
            lastFollowTargetY = target.position.y,
            lastFollowTargetDeltaX = resolveTargetDeltaX(target),
            lastFollowTargetDeltaY = resolveTargetDeltaY(target),
        )

    private fun matches(
        engagement: CombatEngagementState?,
        target: Entity,
        intent: CombatIntent,
    ): Boolean =
        engagement != null &&
            engagement.intent == intent &&
            engagement.targetType == target.type &&
            engagement.targetSlot == target.slot

    private fun applyFacing(player: Client, target: Entity) {
        player.target = target
        if (target is Npc) {
            player.faceNpc(target.slot)
        } else {
            player.facePlayer(target.slot)
        }
    }

    private fun resolveTargetDeltaX(target: Entity): Int = if (target is Client) target.lastWalkDeltaX.coerceIn(-1, 1) else 0

    private fun resolveTargetDeltaY(target: Entity): Int = if (target is Client) target.lastWalkDeltaY.coerceIn(-1, 1) else 0

    private fun isValidTarget(target: Entity): Boolean =
        when (target) {
            is Client -> !target.disconnected && target.currentHealth > 0 && !target.isDeathSequenceActive
            is Npc -> target.alive && target.currentHealth > 0
            else -> false
        }

    private fun inferIntent(target: Entity): CombatIntent =
        when (target) {
            is Npc -> CombatIntent.ATTACK_NPC
            else -> CombatIntent.ATTACK_PLAYER
        }
}
