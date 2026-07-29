package net.dodian.uber.game.combat

import net.dodian.uber.game.engine.systems.animation.AttackAnimationService
import net.dodian.uber.game.engine.systems.combat.CombatAttackResult
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.player.skills.Skill

/** Pure combat-number contract. Strategies may adopt these rolls incrementally. */
interface CombatFormula {
    fun accuracyRoll(attacker: Client, target: Entity): Double
    fun defenceRoll(attacker: Client, target: Entity): Double
    fun maximumHit(attacker: Client, target: Entity): Int
}

enum class CombatStrategyStyle {
    MELEE,
    RANGED,
    MAGIC,
}

data class CombatStrategyProfile(
    val style: CombatStrategyStyle,
    val reach: Int,
    val damageType: Entity.damageType,
    val projectile: Boolean,
)

/** Execution contract around the existing cooldown, animation, projectile and hit-queue runtime. */
interface CombatStrategy {
    val profile: CombatStrategyProfile
    val formula: CombatFormula

    fun cadenceTicks(attacker: Client): Int
    fun attackAnimation(attacker: Client): Int
    fun hitDelayTicks(attacker: Client, target: Entity): Int
    fun execute(attacker: Client): CombatAttackResult?
}

private fun defenceLevel(target: Entity): Int = when (target) {
    is Client -> target.getLevel(Skill.DEFENCE)
    is Npc -> target.defence
    else -> 0
}

private fun defenceBonus(target: Entity, index: Int): Int =
    if (target is Client) target.playerBonus[index] else 0

object MeleeCombatFormula : CombatFormula {
    override fun accuracyRoll(attacker: Client, target: Entity): Double =
        attacker.getLevel(Skill.ATTACK) * (highestAttackBonus(attacker) + 64.0)

    override fun defenceRoll(attacker: Client, target: Entity): Double =
        defenceLevel(target) * (if (target is Client) highestDefensiveBonus(target) + 64.0 else 64.0)

    override fun maximumHit(attacker: Client, target: Entity): Int = attacker.meleeMaxHit()
}

object RangedCombatFormula : CombatFormula {
    override fun accuracyRoll(attacker: Client, target: Entity): Double =
        attacker.getLevel(Skill.RANGED) * (attacker.playerBonus[4] + 64.0)

    override fun defenceRoll(attacker: Client, target: Entity): Double =
        defenceLevel(target) * (defenceBonus(target, 9) + 64.0)

    override fun maximumHit(attacker: Client, target: Entity): Int = attacker.rangedMaxHit()
}

object MagicCombatFormula : CombatFormula {
    override fun accuracyRoll(attacker: Client, target: Entity): Double =
        attacker.getLevel(Skill.MAGIC) * (attacker.playerBonus[3] + 64.0)

    override fun defenceRoll(attacker: Client, target: Entity): Double =
        defenceLevel(target) * (defenceBonus(target, 8) + 64.0)

    override fun maximumHit(attacker: Client, target: Entity): Int {
        val slot = attacker.autocast_spellIndex
        val damage = AncientSpellRegistry.baseDamage()
        return if (slot in damage.indices) {
            (damage[slot] * attacker.magicBonusDamage()).toInt()
        } else {
            0
        }
    }
}

abstract class LegacyCombatStrategy : CombatStrategy {
    override fun cadenceTicks(attacker: Client): Int =
        attacker.getbattleTimer(attacker.equipment[Equipment.Slot.WEAPON.id])

    override fun attackAnimation(attacker: Client): Int = AttackAnimationService.resolve(attacker)

    override fun hitDelayTicks(attacker: Client, target: Entity): Int =
        if (profile.projectile) attacker.getDistanceDelay(
            attacker.distanceToPoint(target.position.x, target.position.y),
            profile.style == CombatStrategyStyle.MAGIC,
        ) else 0
}

object MeleeCombatStrategy : LegacyCombatStrategy() {
    override val profile = CombatStrategyProfile(CombatStrategyStyle.MELEE, 1, Entity.damageType.MELEE, false)
    override val formula: CombatFormula = MeleeCombatFormula
    override fun execute(attacker: Client): CombatAttackResult? = attacker.handleMeleeAttack()
}

object RangedCombatStrategy : LegacyCombatStrategy() {
    override val profile = CombatStrategyProfile(CombatStrategyStyle.RANGED, 5, Entity.damageType.RANGED, true)
    override val formula: CombatFormula = RangedCombatFormula
    override fun execute(attacker: Client): CombatAttackResult? = attacker.handleRangedAttack()
}

object MagicCombatStrategy : LegacyCombatStrategy() {
    override val profile = CombatStrategyProfile(CombatStrategyStyle.MAGIC, 5, Entity.damageType.MAGIC, true)
    override val formula: CombatFormula = MagicCombatFormula
    override fun execute(attacker: Client): CombatAttackResult? = attacker.handleMagicAttack()
}

object PlayerCombatStrategies {
    fun resolve(attackStyle: Int): CombatStrategy? = when (attackStyle) {
        0 -> MeleeCombatStrategy
        1 -> RangedCombatStrategy
        2 -> MagicCombatStrategy
        else -> null
    }
}
