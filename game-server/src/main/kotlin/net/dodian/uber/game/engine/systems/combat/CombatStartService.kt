package net.dodian.uber.game.engine.systems.combat

import net.dodian.uber.game.combat.getAttackStyle
import net.dodian.uber.game.engine.loop.GameCycleClock
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

object CombatStartService {
    @JvmStatic
    fun startPlayerAttack(
        client: Client,
        target: Client,
        intent: CombatIntent = CombatIntent.ATTACK_PLAYER,
    ) {
        CombatCommandService.requestAttack(client, target, intent)
    }

    @JvmStatic
    fun startNpcAttack(
        client: Client,
        target: Npc,
        intent: CombatIntent = CombatIntent.ATTACK_NPC,
    ) {
        CombatCommandService.requestAttack(client, target, intent)
    }

    @JvmStatic
    fun beginAttackNow(
        client: Client,
        target: Entity,
        intent: CombatIntent,
    ): Boolean = CombatCommandService.requestAttack(client, target, intent) != CombatCommandService.AttackRequestResult.REJECTED_INVALID

    @JvmStatic
    fun canPerformAttackTick(client: Client): Boolean = CombatCommandService.markInitialSwingConsumed(client)

    @JvmStatic
    fun clearCombatTarget(client: Client) {
        CombatCommandService.resetCombatFully(
            client,
            client.combatCancellationReason ?: CombatCancellationReason.TARGET_INVALID,
        )
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

    @JvmStatic
    fun restoreCooldownState(
        client: Client,
        cycleNow: Long = GameCycleClock.currentCycle(),
    ): CombatCooldownState = CombatCommandService.ensureCooldownState(client, cycleNow)
}
