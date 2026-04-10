package net.dodian.uber.game.systems.combat

import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.interaction.NpcInteractionIntent
import net.dodian.uber.game.systems.interaction.scheduler.InteractionTaskScheduler
import net.dodian.uber.game.systems.interaction.scheduler.NpcInteractionTask
import net.dodian.uber.game.engine.loop.GameCycleClock

object CombatStartService {
    @JvmStatic
    fun startPlayerAttack(
        client: Client,
        target: Client,
        intent: CombatIntent = CombatIntent.ATTACK_PLAYER,
    ) {
        beginAttackNow(client, target, intent)
    }

    @JvmStatic
    fun startNpcAttack(
        client: Client,
        target: Npc,
        intent: CombatIntent = CombatIntent.ATTACK_NPC,
    ) {
        if (intent == CombatIntent.ATTACK_NPC) {
            val interactionIntent = NpcInteractionIntent(72, GameCycleClock.currentCycle(), target.slot, 5)
            InteractionTaskScheduler.schedule(client, interactionIntent, NpcInteractionTask(client, interactionIntent))
            return
        }
        beginAttackNow(client, target, intent)
    }

    @JvmStatic
    fun beginAttackNow(
        client: Client,
        target: Entity,
        intent: CombatIntent,
    ): Boolean {
        val cycle = GameCycleClock.currentCycle()
        val current = client.combatTargetState
        val style = client.getAttackStyle()
        client.clearCombatCancellationReason()

        if (current != null &&
            current.targetType == target.type &&
            current.targetSlot == target.slot &&
            current.intent == intent &&
            current.startedCycle == cycle
        ) {
            client.resetWalkingQueue()
            client.combatTargetState =
                current.copy(
                    lastInRangeConfirmationCycle = cycle,
                    nextAttackCycle = cycle,
                    lastAttackCycle = current.lastAttackCycle,
                    autoFollowEnabled = true,
                    lastFollowCycle = cycle,
                    lastFollowTargetX = target.position.x,
                    lastFollowTargetY = target.position.y,
                    lastFollowTargetDeltaX = resolveTargetDeltaX(target),
                    lastFollowTargetDeltaY = resolveTargetDeltaY(target),
                )
            return true
        }

        client.resetWalkingQueue()
        client.target = target
        if (target is Npc) {
            client.faceNpc(target.slot)
        } else {
            client.facePlayer(target.slot)
        }
        client.combatTargetState =
            CombatTargetState(
                intent = intent,
                targetType = target.type,
                targetSlot = target.slot,
                startedCycle = cycle,
                lastInRangeConfirmationCycle = cycle,
                attackStyleAtStart = style,
                initialSwingConsumed = false,
                nextAttackCycle = cycle,
                lastAttackCycle = 0L,
                autoFollowEnabled = true,
                lastFollowCycle = cycle,
                lastFollowTargetX = target.position.x,
                lastFollowTargetY = target.position.y,
                lastFollowTargetDeltaX = resolveTargetDeltaX(target),
                lastFollowTargetDeltaY = resolveTargetDeltaY(target),
            )
        return true
    }

    @JvmStatic
    fun canPerformAttackTick(client: Client): Boolean {
        val state = client.combatTargetState ?: return true
        val target = client.target ?: run {
            client.clearCombatTargetState()
            return true
        }
        if (target.type != state.targetType || target.slot != state.targetSlot) {
            client.clearCombatTargetState()
            return true
        }
        if (state.initialSwingConsumed) {
            return true
        }
        val cycle = GameCycleClock.currentCycle()
        if (cycle < state.startedCycle) {
            return false
        }
        client.combatTargetState =
            state.copy(
                lastInRangeConfirmationCycle = cycle,
                initialSwingConsumed = true,
            )
        return true
    }

    @JvmStatic
    fun clearCombatTarget(client: Client) {
        client.clearCombatTargetState()
    }

    @JvmStatic
    fun policyFor(client: Client, intent: CombatIntent): CombatStartPolicy {
        val attackDistance =
            when (intent) {
                CombatIntent.MAGIC_ON_NPC, CombatIntent.MAGIC_ON_PLAYER -> 5
                CombatIntent.ATTACK_PLAYER, CombatIntent.ATTACK_NPC ->
                    if (client.getAttackStyle() == 0) 1 else 5
            }
        return CombatStartPolicy(attackDistance = attackDistance)
    }

    private fun resolveTargetDeltaX(target: Entity): Int = if (target is Client) target.lastWalkDeltaX.coerceIn(-1, 1) else 0

    private fun resolveTargetDeltaY(target: Entity): Int = if (target is Client) target.lastWalkDeltaY.coerceIn(-1, 1) else 0

}
