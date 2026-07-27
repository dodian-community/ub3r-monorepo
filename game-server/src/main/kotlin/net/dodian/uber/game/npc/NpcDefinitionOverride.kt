package net.dodian.uber.game.npc

import net.dodian.uber.game.engine.systems.cache.CacheNpcDefinition
import net.dodian.uber.game.api.interaction.GameContentDsl

data class NpcCacheOverride(
    val id: Int,
    val name: String? = null,
    val examine: String? = null,
) {
    fun applyTo(definition: CacheNpcDefinition) {
        name?.let { definition.name = it }
        examine?.let { definition.examine = it }
    }
}

data class NpcServerPatch(
    val attackAnimation: Int? = null,
    val defenceAnimation: Int? = null,
    val deathAnimation: Int? = null,
    val respawnTicks: Int? = null,
    val attack: Int? = null,
    val strength: Int? = null,
    val defence: Int? = null,
    val hitpoints: Int? = null,
    val ranged: Int? = null,
    val magic: Int? = null,
    val headIcon: Int? = null,
    val transformTo: Int? = null,
    // Future monster fields: aggressive, attackSpeed, maxHit, slayer metadata
    val aggressive: Boolean? = null,
    val alwaysAggressive: Boolean? = null,
    val fightsBack: Boolean? = null,
    val attackSpeed: Int? = null,
    val bossAttackHandler: NpcAttackHandler? = null,
) {
    fun overlay(child: NpcServerPatch): NpcServerPatch =
        NpcServerPatch(
            attackAnimation = child.attackAnimation ?: attackAnimation,
            defenceAnimation = child.defenceAnimation ?: defenceAnimation,
            deathAnimation = child.deathAnimation ?: deathAnimation,
            respawnTicks = child.respawnTicks ?: respawnTicks,
            attack = child.attack ?: attack,
            strength = child.strength ?: strength,
            defence = child.defence ?: defence,
            hitpoints = child.hitpoints ?: hitpoints,
            ranged = child.ranged ?: ranged,
            magic = child.magic ?: magic,
            headIcon = child.headIcon ?: headIcon,
            transformTo = child.transformTo ?: transformTo,
            aggressive = child.aggressive ?: aggressive,
            alwaysAggressive = child.alwaysAggressive ?: alwaysAggressive,
            fightsBack = child.fightsBack ?: fightsBack,
            attackSpeed = child.attackSpeed ?: attackSpeed,
            bossAttackHandler = child.bossAttackHandler ?: bossAttackHandler,
        )
}

data class NpcServerDefinition(
    val id: Int,
    val attackAnimation: Int? = null,
    val defenceAnimation: Int? = null,
    val deathAnimation: Int? = null,
    val respawnTicks: Int? = null,
    val attack: Int? = null,
    val strength: Int? = null,
    val defence: Int? = null,
    val hitpoints: Int? = null,
    val ranged: Int? = null,
    val magic: Int? = null,
    val headIcon: Int? = null,
    val transformTo: Int? = null,
    val aggressive: Boolean = false,
    val alwaysAggressive: Boolean = false,
    val fightsBack: Boolean = true,
    val attackSpeed: Int? = null,
    val bossAttackHandler: NpcAttackHandler? = null,
) {
    fun asPatch(): NpcServerPatch =
        NpcServerPatch(
            attackAnimation = attackAnimation,
            defenceAnimation = defenceAnimation,
            deathAnimation = deathAnimation,
            respawnTicks = respawnTicks,
            attack = attack,
            strength = strength,
            defence = defence,
            hitpoints = hitpoints,
            ranged = ranged,
            magic = magic,
            headIcon = headIcon,
            transformTo = transformTo,
            aggressive = aggressive,
            alwaysAggressive = alwaysAggressive,
            fightsBack = fightsBack,
            attackSpeed = attackSpeed,
            bossAttackHandler = bossAttackHandler,
        )
}

data class NpcDisplayOverride(
    val headIcon: Int? = null,
    val transformTo: Int? = null,
)

@GameContentDsl
class NpcDefinitionOverrideBuilder internal constructor(private val id: Int) {
    var name: String? = null
    var examine: String? = null

    internal fun build(): NpcCacheOverride =
        NpcCacheOverride(
            id = id,
            name = cleanText(name),
            examine = cleanText(examine),
        )
}

@GameContentDsl
class NpcServerDefinitionBuilder internal constructor(private val id: Int? = null) {
    var attackAnimation: Int? = null
    var defenceAnimation: Int? = null
    var deathAnimation: Int? = null
    var respawnTicks: Int? = null
    var attack: Int? = null
    var strength: Int? = null
    var defence: Int? = null
    var hitpoints: Int? = null
    var ranged: Int? = null
    var magic: Int? = null
    var headIcon: Int? = null
    var transformTo: Int? = null
    var aggressive: Boolean? = null
    var alwaysAggressive: Boolean? = null
    var fightsBack: Boolean? = null
    var attackSpeed: Int? = null
    var bossAttackHandler: NpcAttackHandler? = null

    fun stats(
        attack: Int? = null,
        strength: Int? = null,
        defence: Int? = null,
        hitpoints: Int? = null,
        ranged: Int? = null,
        magic: Int? = null,
    ) {
        this.attack = attack ?: this.attack
        this.strength = strength ?: this.strength
        this.defence = defence ?: this.defence
        this.hitpoints = hitpoints ?: this.hitpoints
        this.ranged = ranged ?: this.ranged
        this.magic = magic ?: this.magic
    }

    fun animations(attack: Int? = null, defence: Int? = null, death: Int? = null) {
        this.attackAnimation = attack ?: attackAnimation
        this.defenceAnimation = defence ?: defenceAnimation
        this.deathAnimation = death ?: deathAnimation
    }

    fun respawn(ticks: Int) {
        respawnTicks = ticks
    }

    fun display(headIcon: Int? = null, transformTo: Int? = null) {
        this.headIcon = headIcon ?: this.headIcon
        this.transformTo = transformTo ?: this.transformTo
    }

    internal fun buildDefinition(): NpcServerDefinition =
        NpcServerDefinition(
            id = requireNotNull(id) { "NpcServerDefinitionBuilder needs an id to build a definition" },
            attackAnimation = positive(attackAnimation),
            defenceAnimation = positive(defenceAnimation),
            deathAnimation = positive(deathAnimation),
            respawnTicks = positive(respawnTicks),
            attack = nonNegative(attack),
            strength = nonNegative(strength),
            defence = nonNegative(defence),
            hitpoints = positive(hitpoints),
            ranged = nonNegative(ranged),
            magic = nonNegative(magic),
            headIcon = nonNegative(headIcon),
            transformTo = nonNegative(transformTo),
            aggressive = aggressive ?: false,
            alwaysAggressive = alwaysAggressive ?: false,
            fightsBack = fightsBack ?: true,
            attackSpeed = attackSpeed,
            bossAttackHandler = bossAttackHandler,
        )

    internal fun buildPatch(): NpcServerPatch =
        NpcServerPatch(
            attackAnimation = positive(attackAnimation),
            defenceAnimation = positive(defenceAnimation),
            deathAnimation = positive(deathAnimation),
            respawnTicks = positive(respawnTicks),
            attack = nonNegative(attack),
            strength = nonNegative(strength),
            defence = nonNegative(defence),
            hitpoints = positive(hitpoints),
            ranged = nonNegative(ranged),
            magic = nonNegative(magic),
            headIcon = nonNegative(headIcon),
            transformTo = nonNegative(transformTo),
            aggressive = aggressive,
            alwaysAggressive = alwaysAggressive,
            fightsBack = fightsBack,
            attackSpeed = attackSpeed,
            bossAttackHandler = bossAttackHandler,
        )
}

@GameContentDsl
class NpcCombatBuilder {
    var handler: NpcAttackHandler? = null
    private var defenceHandler: ((NpcDefenceContext) -> Unit)? = null

    fun handler(handler: NpcAttackHandler) {
        this.handler = handler
    }

    fun defend(handler: NpcDefenceContext.() -> Unit) {
        defenceHandler = { context -> handler(context) }
    }

    internal fun build(): NpcCombatProfile? = handler?.let { NpcCombatProfile(it, defenceHandler) }
}

private fun cleanText(value: String?): String? =
    value
        ?.replace('_', ' ')
        ?.trim()
        ?.takeIf { it.isNotEmpty() && !it.equals("no name", true) && !it.equals("null", true) }

private fun positive(value: Int?): Int? = value?.takeIf { it > 0 }
private fun nonNegative(value: Int?): Int? = value?.takeIf { it >= 0 }
