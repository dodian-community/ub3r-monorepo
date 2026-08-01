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
    fun policyFor(client: Client): CombatStartPolicy {
        // attackDistance must track the attacker's *current* style (client.getAttackStyle()),
        // not the intent that started this engagement - magicId resets to -1 after a one-shot
        // spell cast (see Client.handleMagicAttack), so a MAGIC_ON_NPC/MAGIC_ON_PLAYER engagement
        // can fall back to melee mid-fight. Branching on the frozen `intent` here kept the player
        // parked at ranged distance forever after the first cast, even once attacks were melee again.
        val attackDistance = if (client.getAttackStyle() == 0) 1 else 5
        return CombatStartPolicy(attackDistance = attackDistance)
    }

    @JvmStatic
    fun restoreCooldownState(
        client: Client,
        cycleNow: Long = GameCycleClock.currentCycle(),
    ): CombatCooldownState = CombatCommandService.ensureCooldownState(client, cycleNow)
}