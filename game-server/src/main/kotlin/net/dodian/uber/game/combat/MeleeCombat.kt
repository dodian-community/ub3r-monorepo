package net.dodian.uber.game.combat

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.prayer.Prayers
import net.dodian.utilities.Misc
import net.dodian.utilities.Range
import net.dodian.utilities.Utils

var hit = 0
var hit2 = 0

fun Client.handleMeleeAttack(): Int {
    if (hasStaff() && (autocast_spellIndex >= 0 || magicId >= 0))
        return -1
    else if (!hasStaff() && magicId >= 0)
        return -1
    else if (usingBow)
        return -1
    if (combatTimer > 0 || stunTimer > 0 || target == null) //Need this to be a check here!
        return 0
    if (target is Player && duelFight && duelRule[1]) {
        send(SendMessage("Melee has been disabled for this duel!"))
        resetAttack()
        return 0
    }
        combatTimer = getbattleTimer(equipment[Equipment.Slot.WEAPON.id])
        lastCombat = 16
        setFocus(target.position.x, target.position.y)
        var maxHit = meleeMaxHit().toDouble()
         if (target is Npc) { // Slayer damage!
             val npc = Server.npcManager.getNpc(target.slot)
             val name = npc.npcName().lowercase()
             if(getSlayerDamage(npc.id, false) == 1)
                 maxHit *= 1.15
             else if(getSlayerDamage(npc.id, false) == 2)
                 maxHit *= 1.2
             val wolfBane = equipment[Equipment.Slot.WEAPON.id] == 2952 &&  when {
                 name.lowercase().contains("vampyre") -> true
                 name.lowercase().contains("werewolf") -> true
                 else -> false
             }
             if(wolfBane) maxHit *= 2
             val keris = equipment[Equipment.Slot.WEAPON.id] == 10581 &&  when {
                 name.lowercase().contains("kalphite") -> true
                 name.lowercase().contains("scarab") -> true
                 name.lowercase().contains("spider") -> true
                 else -> false
             }
             if(keris) maxHit *= 2
         }
        hit = Utils.random(maxHit.toInt())
        val criticalChance = getLevel(Skill.AGILITY) / 9
        val extra = getLevel(Skill.STRENGTH) * 0.195
        if(equipment[Equipment.Slot.SHIELD.id]==4224) criticalChance * 1.5
        val landCrit = Math.random() * 100 <= criticalChance
        val landHit = landHit(this, target)
        if(landHit && hit < 1) hit = 1 //Osrs style of hit, max hit is 1 or 1 - maxHit if you land a hit!
        if (target is Npc) {
            val npc = Server.npcManager.getNpc(target.slot)
            val name = npc.npcName().lowercase()
            val wolfBane = equipment[Equipment.Slot.WEAPON.id] == 2952 && landHit && when {
                name.lowercase().contains("vampyre") -> true
                name.lowercase().contains("werewolf") -> true
                else -> false
            }
            val keris = equipment[Equipment.Slot.WEAPON.id] == 10581 && landHit && when {
                name.lowercase().contains("kalphite") -> true
                name.lowercase().contains("scarab") -> true
                name.lowercase().contains("locust") -> true
                name.lowercase().contains("spider") -> true
                else -> false
            }
            if (landCrit && landHit) hit + Utils.dRandom2(extra).toInt()
            else if(!landHit) hit = 0
            if(wolfBane && Misc.chance(8) == 1) {
                hit *= 2
                send(SendMessage("<col=8B4513>You use the power of the wolf to hit higher!"))
            } //#FFD700
            if(keris && Misc.chance(8) == 1) {
                hit *= 2
                send(SendMessage("<col=8B4513>You use the power of the keris to hit higher!"))
            } else if (keris && Misc.chance(33) == 1) {
                hit *= 4
                send(SendMessage("<col=8B4513>You punch a hole in the creatures exoskeleton!"))
            }
            if(!handleSpecial(landCrit)) { //Do stuff here!
                if(hit >= npc.currentHealth) hit = npc.currentHealth
                npc.dealDamage(this, hit, if(landCrit && hit > 0) Entity.hitType.CRIT else Entity.hitType.STANDARD)
            }
            var chance = Misc.chance(8) == 1 && armourSet("guthan")
            if(chance && hit > 0) { //Guthan effect!
                stillgfx(398, npc.position, 100)
                heal(hit, (getMaxHealth().toDouble() * 0.15).toInt())
            }
            chance = Misc.chance(8) == 1 && armourSet("torag")
            if(chance) { //Torag effect!
                hit2 = hit / 2
                stillgfx(399, npc.position, 0)
                if(hit2 >= npc.currentHealth) hit2 = npc.currentHealth
                npc.dealDamage(this, hit2, if(landCrit && hit2 > 0) Entity.hitType.CRIT else Entity.hitType.STANDARD)
            }
            if(hit > 0) {
                if (fightType == 3) {
                    val xp = (13 * hit)
                    giveExperience(xp, Skill.ATTACK)
                    giveExperience(xp, Skill.DEFENCE)
                    giveExperience(xp, Skill.STRENGTH)
                } else giveExperience(40 * hit, Skill.getSkill(fightType))
                giveExperience(13 * hit, Skill.HITPOINTS)
            }
            if(hit2 > 0) {
                if (fightType == 3) {
                    val xp = (13 * hit2)
                    giveExperience(xp, Skill.ATTACK)
                    giveExperience(xp, Skill.DEFENCE)
                    giveExperience(xp, Skill.STRENGTH)
                } else giveExperience(40 * hit2, Skill.getSkill(fightType))
                giveExperience(13 * hit2, Skill.HITPOINTS)
            }
        }
        if (target is Player) {
            val player = Server.playerHandler.getClient(target.slot)
            if (landCrit && landHit) hit + Utils.dRandom2(extra).toInt()
            else if(!landHit) hit = 0
            if (player.prayerManager.isPrayerOn(Prayers.Prayer.PROTECT_MELEE)) (hit * 0.6).toInt()
            if(!handleSpecial(landCrit)) { //Do stuff here!
                if(hit >= player.currentHealth) hit = player.currentHealth
                player.dealDamage(this, hit, if(landCrit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
            }
            var chance = Misc.chance(8) == 1 && armourSet("guthan")
            if(chance && hit > 0) { //Guthan effect!
                stillgfx(398, player.position, 100)
                heal(hit, (getMaxHealth().toDouble() * 0.15).toInt())
            }
            chance = Misc.chance(8) == 1 && armourSet("torag")
            if(chance) { //Torag effect!
                hit2 = hit / 2
                stillgfx(399, player.position, 0)
                if(hit2 >= player.currentHealth) hit2 = player.currentHealth
                player.dealDamage(this, hit2, Entity.hitType.STANDARD)
            }
        }
        if (debug) send(SendMessage("hit = $hit, elapsed = $combatTimer"))
    return 1
}

fun highestAttackBonus(p: Client): Int {
    var bonus = 0
    for (i in 0..2) {
        if (p.playerBonus[i] > bonus)
            bonus = p.playerBonus[i]
    }
    return bonus
    }
    fun highestDefensiveBonus(p: Client): Int {
        var bonus = 0
        for (i in 5..7) {
            if (p.playerBonus[i] > bonus)
                bonus = p.playerBonus[i]
        }
            return bonus
        }
fun landHit(p: Client, t: Entity): Boolean {
    val hitChance: Double
    val chance = Misc.chance(100_000) / 1_000
    val prayerBonus = if(p.prayerManager.isPrayerOn(Prayers.Prayer.CLARITY_OF_THOUGHT)) 1.05
    else if(p.prayerManager.isPrayerOn(Prayers.Prayer.IMPROVED_REFLEXES)) 1.1
    else if(p.prayerManager.isPrayerOn(Prayers.Prayer.INCREDIBLE_REFLEXES)) 1.15
    else if(p.prayerManager.isPrayerOn(Prayers.Prayer.CHIVALRY)) 1.18
    else if(p.prayerManager.isPrayerOn(Prayers.Prayer.PIETY)) 1.22
    else 1.0
    if(t is Client) { //Pvp
        var atkLevel = p.getLevel(Skill.ATTACK)
        val atkBonus = highestAttackBonus(p)
        var defLevel = t.getLevel(Skill.DEFENCE)
        val defBonus = highestDefensiveBonus(t)
        val prayerDefBonus = if(t.prayerManager.isPrayerOn(Prayers.Prayer.THICK_SKIN)) 1.05
        else if(t.prayerManager.isPrayerOn(Prayers.Prayer.ROCK_SKIN)) 1.1
        else if(t.prayerManager.isPrayerOn(Prayers.Prayer.STEEL_SKIN)) 1.15
        else if(t.prayerManager.isPrayerOn(Prayers.Prayer.CHIVALRY)) 1.18
        else if(t.prayerManager.isPrayerOn(Prayers.Prayer.PIETY)) 1.22
        else 1.0
        /* Various bonuses for styles! */
        if(p.fightType == 0) atkLevel += 3
        if(t.fightType == 1) defLevel += 3
        if(p.fightType == 3) atkLevel += 1
        if(t.fightType == 3) defLevel += 1
        /* Calculations */
        val playerDef = (defLevel * (defBonus + 64.0)) * prayerDefBonus
        val playerAccuracy = (atkLevel * (atkBonus + 64.0)) * prayerBonus
        hitChance = if (playerAccuracy > playerDef)
            1 - ((playerDef + 2) / (2 * (playerAccuracy + 1)))
        else
            playerAccuracy / (2 * (playerDef + 1))
        p.debug("Melee Accuracy Hit: " + (hitChance * 100.0) + "% out of " + chance.toDouble() + "%")
        return chance < (hitChance*100)
    } else if(t is Npc) { //Pve
        val atkBonus = highestAttackBonus(p)
        var atkLevel = p.getLevel(Skill.ATTACK)
        val defLevel = t.defence
        val defBonus = 0.0
        val npcDef = (defLevel + 9) * (defBonus + 64.0)
        /* Various bonuses for styles! */
        if(p.fightType == 0) atkLevel += 3
        if(p.fightType == 3) atkLevel += 1
        /* Calculation */
        var playerAccuracy = (atkLevel * (atkBonus + 64.0)) * prayerBonus
        playerAccuracy = if(p.getSlayerDamage(t.id, false) == 1) playerAccuracy * 1.15
        else if(p.getSlayerDamage(t.id, false) == 2) playerAccuracy * 1.20 else playerAccuracy
        hitChance = if (playerAccuracy > npcDef)
            1 - ((npcDef + 2) / (2 * (playerAccuracy + 1)))
        else
            playerAccuracy / (2 * (npcDef + 1))
        p.debug("Melee Accuracy Hit: " + (hitChance * 100.0) + "% out of " + chance.toDouble() + "%%")
        return chance < (hitChance*100)
    }
    return true
}

fun Client.handleSpecial(crit: Boolean): Boolean {
    val emote = Server.itemManager.getAttackAnim(equipment[Equipment.Slot.WEAPON.id])
    val chance = Range(1, 8).value
    if(chance != 1 || hit == 0) { //Do not occur special attack if hit a 0 or chance is 0!
        sendAnimation(emote)
    } else if (target is Npc) {
        val npc = Server.npcManager.getNpc(target.slot)
        when (equipment[Equipment.Slot.WEAPON.id]) {
            1215 -> {
                hit = (hit * 1.1).toInt()
                hit2 = Range(1, hit / 2).value
                callGfxMask(252, 100)
                sendAnimation(1062)
                /* Damage portion! */
                if(hit >= npc.currentHealth) hit = npc.currentHealth
                    npc.dealDamage(this, hit, if(crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
                if (hit2 >= npc.currentHealth) hit2 = npc.currentHealth
                    npc.dealDamage(this, hit2, Entity.hitType.STANDARD)
                return true
            }
            else -> sendAnimation(emote)
        }
    } else if (target is Player) {
        val player = Server.playerHandler.getClient(target.slot)
        when (equipment[Equipment.Slot.WEAPON.id]) {
            1215 -> {
                hit = (hit * 1.1).toInt()
                hit2 = Range(1, hit / 2).value
                callGfxMask(252, 100)
                sendAnimation(1062)
                /* Damage portion! */
                if(hit >= player.currentHealth) hit = player.currentHealth
                    player.dealDamage(this, hit, if(crit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
                if (hit2 >= player.currentHealth) hit2 = player.currentHealth
                    player.dealDamage(this, hit2, Entity.hitType.STANDARD)
                return true
            }
            else ->  sendAnimation(emote)
        }
    }
    return false
}