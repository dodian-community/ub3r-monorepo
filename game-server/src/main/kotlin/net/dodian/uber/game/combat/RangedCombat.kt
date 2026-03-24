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
import net.dodian.uber.game.model.player.skills.prayer.Prayers
import net.dodian.uber.game.runtime.animation.PlayerAnimationService
import net.dodian.uber.game.runtime.combat.CombatAttackResult
import net.dodian.uber.game.runtime.combat.CombatHitQueueService
import net.dodian.uber.game.runtime.combat.CombatLogoutLockService
import net.dodian.uber.game.skills.core.progression.SkillProgressionService
import net.dodian.utilities.Misc
import net.dodian.utilities.Utils

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

    val arrows = mapOf(
        882 to listOf(10, 19),
        884 to listOf(9, 18),
        886 to listOf(11, 20),
        888 to listOf(12, 21),
        890 to listOf(13, 22),
        892 to listOf(14, 23),
        11212 to listOf(1120, 1116)
    )
    //obby ring, 442 + 443
    val equippedArrow = equipment[Equipment.Slot.ARROWS.id]
    var arrowGfx = arrows[equippedArrow]?.get(0) ?: 10
    var arrowPullGfx = arrows[equippedArrow]?.get(1) ?: 20
    val emote = Server.itemManager.getAttackAnim(equipment[Equipment.Slot.WEAPON.id])

    if(equipment[Equipment.Slot.WEAPON.id] == 4734) {
        arrowGfx = 27
        arrowPullGfx = -1 //Not right but believe there is no real gfx for this!
    }

    CombatLogoutLockService.refreshInteraction(this, target)
    setFocus(target.position.x, target.position.y)
    val distance = distanceToPoint(target.position.x, target.position.y)
    val hitDelay = getDistanceDelay(distance, false).toLong()
    if (DeleteArrow()) {
        if(target is Npc) {
            val offsetX = (position.y - target.position.y) * 1
            val offsetY = (position.x - target.position.x) * 1
            PlayerAnimationService.requestResetClear(this)
            PlayerAnimationService.requestAttack(this, emote)
            callGfxMask(arrowPullGfx, 100)
            arrowGfx(offsetY, offsetX, 50, 50 + (distance * 5), arrowGfx, 43, 35, target.slot + 1, 51, 16)
        } else {
            val offsetX = (position.y - target.position.y) * -1
            val offsetY = (position.x - target.position.x) * -1
            PlayerAnimationService.requestResetClear(this)
            PlayerAnimationService.requestAttack(this, emote)
            callGfxMask(arrowPullGfx, 100)
            arrowGfx(offsetY, offsetX, 50, 50 + (distance * 5), arrowGfx, 43, 35, -(target.slot + 1), 51, 16)
        }
    }
    var maxHit = rangedMaxHit().toDouble()
    if (target is Npc) { // Slayer damage!
        val npcId = Server.npcManager.getNpc(target.slot).id
        if(getSlayerDamage(npcId, true) == 2)
            maxHit *= 1.2
    }
    var hit = Utils.random(maxHit.toInt())
    val criticalChance = getLevel(Skill.AGILITY) / 9
    val extra = getLevel(Skill.RANGED) * 0.195
    if(equipment[Equipment.Slot.SHIELD.id]==4224) criticalChance * 1.5
    val landCrit = Math.random() * 100 <= criticalChance
    val landHit = landHitRanged(this, target)
    if(landHit && hit < 1) hit = 1 //Osrs style of hit, max hit is 1 or 1 - maxHit if you land a hit!
    if (target is Npc) {
        val npc = Server.npcManager.getNpc(target.slot)
        if (landCrit && landHit)
            hit + Utils.dRandom2(extra).toInt()
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
                SkillProgressionService.gainXp(this, xp, Skill.DEFENCE)
                SkillProgressionService.gainXp(this, xp, Skill.RANGED)
            } else SkillProgressionService.gainXp(this, 40 * hit, Skill.RANGED)
            SkillProgressionService.gainXp(this, 13 * hit, Skill.HITPOINTS)
        }
        if(hit2 > 0) {
            if (fightType == 1) {
                val xp = (20 * hit2)
                SkillProgressionService.gainXp(this, xp, Skill.DEFENCE)
                SkillProgressionService.gainXp(this, xp, Skill.RANGED)
            } else SkillProgressionService.gainXp(this, 40 * hit2, Skill.RANGED)
            SkillProgressionService.gainXp(this, 13 * hit2, Skill.HITPOINTS)
        }
    }
    if (target is Player) {
        val player = Server.playerHandler.getClient(target.slot)
        if (landCrit && landHit)
            hit + Utils.dRandom2(extra).toInt()
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
    val prayerBonus = if(p.prayerManager.isPrayerOn(Prayers.Prayer.SHARP_EYE)) 1.025
    else if(p.prayerManager.isPrayerOn(Prayers.Prayer.HAWK_EYE)) 1.05
    else if(p.prayerManager.isPrayerOn(Prayers.Prayer.EAGLE_EYE)) 1.075
    else 1.0
    if(t is Client) { //Pvp
        val atkBonus = p.playerBonus[4]
        var atkLevel = p.getLevel(Skill.RANGED)
        val defBonus = t.playerBonus[9]
        var defLevel = t.getLevel(Skill.DEFENCE)
        val prayerDefBonus = if(t.prayerManager.isPrayerOn(Prayers.Prayer.THICK_SKIN)) 1.05
        else if(p.prayerManager.isPrayerOn(Prayers.Prayer.ROCK_SKIN)) 1.1
        else if(p.prayerManager.isPrayerOn(Prayers.Prayer.STEEL_SKIN)) 1.15
        else if(p.prayerManager.isPrayerOn(Prayers.Prayer.CHIVALRY)) 1.18
        else if(p.prayerManager.isPrayerOn(Prayers.Prayer.PIETY)) 1.22
        else 1.0
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
