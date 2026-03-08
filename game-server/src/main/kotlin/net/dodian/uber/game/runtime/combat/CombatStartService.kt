package net.dodian.uber.game.runtime.combat

import net.dodian.uber.game.combat.getAttackStyle
import net.dodian.uber.game.event.GameEventScheduler
import net.dodian.uber.game.model.WalkToTask
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.interaction.NpcInteractionIntent
import net.dodian.uber.game.runtime.interaction.scheduler.InteractionTaskScheduler
import net.dodian.uber.game.runtime.interaction.scheduler.NpcInteractionTask
import net.dodian.uber.game.runtime.loop.GameCycleClock
import net.dodian.uber.game.runtime.scheduler.QueueTaskService
import net.dodian.utilities.queueTasksEnabled

object CombatStartService {
    @JvmStatic
    fun startPlayerAttack(
        client: Client,
        target: Client,
        intent: CombatIntent = CombatIntent.ATTACK_PLAYER,
    ) {
        val policy = policyFor(client, intent)
        if (canAttackNow(client, target, policy)) {
            beginAttackNow(client, target, intent)
            return
        }

        val task = WalkToTask(WalkToTask.Action.ATTACK_PLAYER, target.slot, target.position)
        client.walkToTask = task
        scheduleWalkToAttack(client, task) {
            if (target.disconnected) {
                return@scheduleWalkToAttack false
            }
            if (canAttackNow(client, target, policy)) {
                beginAttackNow(client, target, intent)
                client.walkToTask = null
                return@scheduleWalkToAttack false
            }
            true
        }
    }

    @JvmStatic
    fun startNpcAttack(
        client: Client,
        target: Npc,
        intent: CombatIntent = CombatIntent.ATTACK_NPC,
    ) {
        val policy = policyFor(client, intent)
        if (canAttackNow(client, target, policy)) {
            beginAttackNow(client, target, intent)
            return
        }

        if (intent == CombatIntent.ATTACK_NPC) {
            val interactionIntent = NpcInteractionIntent(72, GameCycleClock.currentCycle(), target.slot, 5)
            InteractionTaskScheduler.schedule(client, interactionIntent, NpcInteractionTask(client, interactionIntent))
            return
        }

        val task = WalkToTask(WalkToTask.Action.ATTACK_NPC, target.slot, target.position)
        client.walkToTask = task
        scheduleWalkToAttack(client, task) {
            if (!target.alive) {
                return@scheduleWalkToAttack false
            }
            if (canAttackNow(client, target, policy)) {
                beginAttackNow(client, target, intent)
                client.walkToTask = null
                return@scheduleWalkToAttack false
            }
            true
        }
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
                    autoFollowEnabled = true,
                    lastFollowCycle = cycle,
                    lastFollowTargetX = target.position.x,
                    lastFollowTargetY = target.position.y,
                )
            return true
        }

        client.resetWalkingQueue()
        client.walkToTask = null
        client.startAttack(target)
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
                autoFollowEnabled = true,
                lastFollowCycle = cycle,
                lastFollowTargetX = target.position.x,
                lastFollowTargetY = target.position.y,
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

    private fun canAttackNow(
        client: Client,
        target: Entity,
        policy: CombatStartPolicy,
    ): Boolean = client.goodDistanceEntity(target, policy.attackDistance)

    private fun scheduleWalkToAttack(
        client: Client,
        task: WalkToTask,
        step: () -> Boolean,
    ) {
        if (queueTasksEnabled) {
            QueueTaskService.schedule(1, 1) {
                if (client.disconnected || client.walkToTask != task) {
                    return@schedule false
                }
                step()
            }
            return
        }

        GameEventScheduler.runRepeatingMs(600) {
            if (client.disconnected || client.walkToTask != task) {
                return@runRepeatingMs false
            }
            step()
        }
    }
}
