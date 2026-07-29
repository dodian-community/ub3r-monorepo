package net.dodian.uber.game.combat

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.Skills
import net.dodian.uber.game.engine.systems.skills.asSkillPlayer
import net.dodian.uber.skills.prayer.PrayerCombatService
import net.dodian.uber.skills.agility.AgilityCombatService
import net.dodian.uber.game.engine.systems.animation.PlayerAnimationService
import net.dodian.uber.game.engine.systems.combat.CombatAttackResult
import net.dodian.uber.game.engine.systems.combat.CombatHitQueueService
import net.dodian.uber.game.engine.systems.combat.CombatLogoutLockService
import net.dodian.uber.game.engine.systems.combat.resolveCombatTargetPlayer
import net.dodian.uber.game.engine.systems.skills.ProgressionService
import net.dodian.uber.game.engine.util.Misc
import net.dodian.utilities.Utils
import net.dodian.uber.game.engine.systems.cache.CacheSpotAnimDefinitions

private val arrowGfxNames = mapOf(
    882 to listOf("bronze_arrow_travel", "bronze_arrow_launch"),
    884 to listOf("iron_arrow_travel", "iron_arrow_launch"),
    886 to listOf("steel_arrow_travel", "steel_arrow_launch"),
    888 to listOf("mithril_arrow_travel", "mithril_arrow_launch"),
    890 to listOf("adamant_arrow_travel", "adamant_arrow_launch"),
    892 to listOf("rune_arrow_travel", "rune_arrow_launch"),
    11212 to listOf("ii_dragon_arrow_normal_projanim", "dragon_arrow_launch")
)

fun Client.handleRangedAttack(): CombatAttackResult? {
    if (stunTimer > 0 || target == null)
        return null
    if(goodDistanceEntity(target, 5))
        resetWalkingQueue()

    if(equipmentN[Equipment.Slot.ARROWS.id] < 1) {
        deleteequiment(equipment[Equipment.Slot.ARROWS.id], Equipment.Slot.ARROWS.id)
        resetAttack()
        send(SendMessage("You're out of arrows!"))
        return null
    }

    //obby ring, 442 + 443
    val equippedArrow = equipment[Equipment.Slot.ARROWS.id]
    var arrowGfx = arrowGfxNames[equippedArrow]?.get(0) ?: "bronze_arrow_travel"
    var arrowPullGfx: String? = arrowGfxNames[equippedArrow]?.get(1) ?: "bronze_arrow_launch"
    val emote = net.dodian.uber.game.engine.systems.animation.AttackAnimationService.resolve(this)
    // TODO: Ranged special attacks — magic shortbow (anim 1074, gfx 256, 2 hits),
    // dark bow (2 hits, 1.3x/1.5x dmg), dragon thrownaxe (anim 7521, instant),
    // dragon knife (anim 8291, 2 hits), ballista (anim 7222, 1.25x acc+dmg)
    // if (specialActivated) { ... handle ranged spec per weapon ID }

    if(equipment[Equipment.Slot.WEAPON.id] == 4734) {
        arrowGfx = "crossbowbolt_travel"
        arrowPullGfx = null
    }

    CombatLogoutLockService.refreshInteraction(this, target)
    if (target is Player) {
        facePlayer(target.slot)
    } else {
        setFocus(target.position.x, target.position.y)
    }
    val distance = distanceToPoint(target.position.x, target.position.y)
    val hitDelay = getDistanceDelay(distance, false).toLong()
    if (DeleteArrow()) {
        PlayerAnimationService.requestResetClear(this)
        PlayerAnimationService.requestAttack(this, emote)
        if (arrowPullGfx != null) {
            val arrowPullGfxId = SpotAnimNames.getId(arrowPullGfx)
            if (arrowPullGfxId != -1) {
                callGfxMask(arrowPullGfxId, 100)
            }
        }
        this.shoot(arrowGfx, target)
    }
    var maxHit = rangedMaxHit().toDouble()
    if (target is Npc) { // Slayer damage!
        val npcId = Server.npcManager.getNpc(target.slot).id
        if(getSlayerDamage(npcId, true) == 2)
            maxHit *= 1.2
    }
    var hit = Utils.random(maxHit.toInt())
    var criticalChance = AgilityCombatService.criticalChance(asSkillPlayer())
    val extra = getLevel(Skill.RANGED) * 0.195
    if(equipment[Equipment.Slot.SHIELD.id]==4224) criticalChance = (criticalChance * 1.5).toInt()
    val landCrit = Math.random() * 100 <= criticalChance
    val landHit = landHitRanged(this, target)
    if(landHit && hit < 1) hit = 1 //Osrs style of hit, max hit is 1 or 1 - maxHit if you land a hit!
    if (target is Npc) {
        val npc = Server.npcManager.getNpc(target.slot)
        if (landCrit && landHit)
            hit += Utils.dRandom2(extra).toInt()
        else if(!landHit) hit = 0
        CombatHitQueueService.enqueue(
            currentGameCycle + hitDelay,
            this,
            npc,
            hit,
            if(landCrit) Entity.hitType.CRIT else Entity.hitType.STANDARD,
            Entity.damageType.RANGED,
        )

        var hit2 = hit
        val chance = Misc.chance(8) == 1 && armourSet("karil")
        if(chance && hit2 > 0) { //Karil effect!
            stillgfx(401, npc.position, 100)
            hit2 = (hit2 * (Skills.getLevelForExperience(getExperience(Skill.RANGED)).toDouble() / 100.0)).toInt()
            CombatHitQueueService.enqueue(
                currentGameCycle + hitDelay,
                this,
                npc,
                hit2,
                if(landCrit) Entity.hitType.CRIT else Entity.hitType.STANDARD,
                Entity.damageType.RANGED,
            )
        }
        /* Experience */
        if(hit > 0) {
            if (fightType == 1) {
                val xp = (20 * hit)
                ProgressionService.addXp(this, xp, Skill.DEFENCE)
                ProgressionService.addXp(this, xp, Skill.RANGED)
            } else ProgressionService.addXp(this, 40 * hit, Skill.RANGED)
            ProgressionService.addXp(this, 13 * hit, Skill.HITPOINTS)
        }
        if(hit2 > 0) {
            if (fightType == 1) {
                val xp = (20 * hit2)
                ProgressionService.addXp(this, xp, Skill.DEFENCE)
                ProgressionService.addXp(this, xp, Skill.RANGED)
            } else ProgressionService.addXp(this, 40 * hit2, Skill.RANGED)
            ProgressionService.addXp(this, 13 * hit2, Skill.HITPOINTS)
        }
    }
    if (target is Player) {
        val player = resolveCombatTargetPlayer(target.slot) ?: return CombatAttackResult(getbattleTimer(equipment[Equipment.Slot.WEAPON.id]))
        if (landCrit && landHit)
            hit += Utils.dRandom2(extra).toInt()
        else if (!landHit) hit = 0
        CombatHitQueueService.enqueue(
            currentGameCycle + hitDelay,
            this,
            player,
            hit,
            if(landCrit) Entity.hitType.CRIT else Entity.hitType.STANDARD,
            Entity.damageType.RANGED,
        )

        var hit2 = hit
        val chance = Misc.chance(8) == 1 && armourSet("karil")
        if (chance && hit2 > 0) { //Karil effect!
            stillgfx(401, player.position, 100)
            hit2 = (hit2 * (Skills.getLevelForExperience(getExperience(Skill.RANGED)).toDouble() / 100.0)).toInt()
            CombatHitQueueService.enqueue(
                currentGameCycle + hitDelay,
                this,
                player,
                hit2,
                if(landCrit) Entity.hitType.CRIT else Entity.hitType.STANDARD,
                Entity.damageType.RANGED,
            )
        }
    }
    val nextDelay = getbattleTimer(equipment[Equipment.Slot.WEAPON.id])
    if (debug) send(SendMessage("hit = $hit, nextDelay = $nextDelay, hitDelay = $hitDelay"))
    return CombatAttackResult(nextDelay)
}

fun landHitRanged(p: Client, t: Entity): Boolean {
    val hitChance: Double
    val chance = Misc.chance(100_000) / 1_000
    val prayerBonus = PrayerCombatService.rangedAttackMultiplier(p.asSkillPlayer())
    if(t is Client) { //Pvp
        val atkBonus = p.playerBonus[4]
        var atkLevel = p.getLevel(Skill.RANGED)
        val defBonus = t.playerBonus[9]
        var defLevel = t.getLevel(Skill.DEFENCE)
        val prayerDefBonus = PrayerCombatService.defenceMultiplier(t.asSkillPlayer())
        /* Various bonuses for styles! */
        if(p.fightType == 0) atkLevel += 3
        if(t.fightType == 1) defLevel += 3
        /* Calculation */
        val playerDef = (defLevel * (defBonus + 64.0)) * prayerDefBonus
        val playerAccuracy = (atkLevel * (atkBonus + 64.0)) * prayerBonus
        hitChance = if (playerAccuracy > playerDef)
            1 - ((playerDef + 2) / (2 * (playerAccuracy + 1)))
        else
            playerAccuracy / (2 * (playerDef + 1))
        p.debug("Ranged Accuracy Hit: " + (hitChance * 100.0) + "% out of " + chance.toDouble() + "%")
        return chance < (hitChance*100)
    } else if(t is Npc) { //Pve
        val atkBonus = p.playerBonus[4]
        var atkLevel = p.getLevel(Skill.RANGED)
        val defLevel = t.defence
        val defBonus = 0.0
        /* Various bonuses for styles! */
        if(p.fightType == 0) atkLevel += 3
        /* Calculation */
        val npcDef = (defLevel + 9) * (defBonus + 64.0)
        var playerAccuracy = (atkLevel * (atkBonus + 64.0)) * prayerBonus
        playerAccuracy = if(p.getSlayerDamage(t.id, true) == 2) playerAccuracy * 1.2 else playerAccuracy
        hitChance = if (playerAccuracy > npcDef)
            1 - ((npcDef + 2) / (2 * (playerAccuracy + 1)))
        else
            playerAccuracy / (2 * (npcDef + 1))
        p.debug("Ranged Accuracy Hit: " + (hitChance * 100.0) + "% out of " + chance.toDouble() + "%")
        return chance < (hitChance*100)
    }
    return true
}
