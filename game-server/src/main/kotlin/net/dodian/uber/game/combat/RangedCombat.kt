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
import net.dodian.utilities.Utils

fun Client.handleRanged(): Int {
    if (combatTimer > 0) //Need this to be a check here!
        return 0
    if (target is Player && duelFight && duelRule[0]) {
        send(SendMessage("Ranged has been disabled for this duel!"))
        resetAttack()
        return 0
    }
    if(equipmentN[Equipment.Slot.ARROWS.id] < 1) {
        deleteequiment(equipment[Equipment.Slot.ARROWS.id], Equipment.Slot.ARROWS.id)
        resetAttack()
        send(SendMessage("You're out of arrows!"))
        return 0
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
    val equippedArrow = equipment[Equipment.Slot.ARROWS.id]
    val arrowGfx = arrows[equippedArrow]?.get(0) ?: 10
    val arrowPullGfx = arrows[equippedArrow]?.get(1) ?: 20

    combatTimer = getbattleTimer(equipment[Equipment.Slot.WEAPON.id]);
    lastCombat = 16
    setFocus(target.position.x, target.position.y)
    if (DeleteArrow()) {
        val distance = distanceToPoint(target.position.x, target.position.y)
        if(target is Npc) {
            val offsetX = (position.y - target.position.y) * 1
            val offsetY = (position.x - target.position.x) * 1
            sendAnimation(426)
            callGfxMask(arrowPullGfx, 100)
            arrowGfx(offsetY, offsetX, 50, 50 + (distance * 5), arrowGfx, 43, 35, target.slot + 1, 51, 16)
        } else {
            val offsetX = (position.y - target.position.y) * -1
            val offsetY = (position.x - target.position.x) * -1
            sendAnimation(426)
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
    if(equipment[Equipment.Slot.SHIELD.id]==4224)
        criticalChance * 1.5
    val extra = getLevel(Skill.RANGED) * 0.195
    val landCrit = Math.random() * 100 <= criticalChance
    val landHit = landHitRanged(this, target)
    if (target is Npc) {
        val npc = Server.npcManager.getNpc(target.slot)
        if (landCrit && landHit)
            hit + Utils.dRandom2(extra).toInt()
        else if(!landHit) hit = 0
        if(hit >= npc.currentHealth)
            hit = npc.currentHealth
        npc.dealDamage(this, hit, landCrit)
    }
    if (target is Player) {
        val player = Server.playerHandler.getClient(target.slot)
        if (landCrit && landHit)
            hit + Utils.dRandom2(extra).toInt()
        else if(!landHit) hit = 0
        if (player.prayerManager.isPrayerOn(Prayers.Prayer.PROTECT_RANGE)) hit /= 2
        if(hit >= player.currentHealth)
            hit = player.currentHealth
        player.dealDamage(hit, landCrit)
    }

    if(target is Npc) {
        val xp = (if (FightType != 3) 40 * hit else 20 * hit) * CombatExpRate
        giveExperience(xp, Skill.RANGED)
        if (FightType == 3) giveExperience(xp, Skill.DEFENCE)
        giveExperience((15 * hit) * CombatExpRate, Skill.HITPOINTS)
    }

    if (debug) send(SendMessage("hit = $hit, elapsed = ${combatTimer}"))
    return 1
}

fun landHitRanged(p: Client, t: Entity): Boolean {
    val hitChance: Double
    val chance = Misc.chance(100000) / 1000
    val prayerBonus = if(p.prayerManager.isPrayerOn(Prayers.Prayer.SHARP_EYE)) 1.025
    else if(p.prayerManager.isPrayerOn(Prayers.Prayer.HAWK_EYE)) 1.05
    else if(p.prayerManager.isPrayerOn(Prayers.Prayer.EAGLE_EYE)) 1.075
    else 1.0
    if(t is Client) { //Pvp
        val atkBonus = p.playerBonus[4]
        val atkLevel = p.getLevel(Skill.RANGED)
        val defBonus = t.playerBonus[9]
        val defLevel = t.getLevel(Skill.DEFENCE)
        val prayerDefBonus = if(t.prayerManager.isPrayerOn(Prayers.Prayer.THICK_SKIN)) 1.05
        else if(p.prayerManager.isPrayerOn(Prayers.Prayer.ROCK_SKIN)) 1.1
        else if(p.prayerManager.isPrayerOn(Prayers.Prayer.STEEL_SKIN)) 1.15
        else if(p.prayerManager.isPrayerOn(Prayers.Prayer.CHIVALRY)) 1.18
        else if(p.prayerManager.isPrayerOn(Prayers.Prayer.PIETY)) 1.22
        else 1.0
        val playerDef = (defLevel * (defBonus + 64.0)) * prayerDefBonus
        val playerAccuracy = (atkLevel * (atkBonus + 64.0)) * prayerBonus
        if (playerAccuracy > playerDef)
            hitChance = 1 - ((playerDef + 2) / (2 * (playerAccuracy + 1)))
        else
            hitChance = playerAccuracy / (2 * (playerDef + 1))
        p.debug("Ranged Accuracy Hit: " + (hitChance * 100.0) + "% out of " + chance.toDouble() + "%")
        return chance < (hitChance*100)
    } else if(t is Npc) { //Pve
        val atkBonus = p.playerBonus[4]
        val atkLevel = p.getLevel(Skill.RANGED)
        val defLevel = t.defence
        val defBonus = 0.0
        val npcDef = defLevel * (defBonus + 64.0)
        var playerAccuracy = (atkLevel * (atkBonus + 64.0)) * prayerBonus
        playerAccuracy = if(p.getSlayerDamage(t.id, true) == 2) playerAccuracy * 1.2 else playerAccuracy
        if (playerAccuracy > npcDef)
            hitChance = 1 - ((npcDef + 2) / (2 * (playerAccuracy + 1)))
        else
            hitChance = playerAccuracy / (2 * (npcDef + 1))
        p.debug("Ranged Accuracy Hit: " + (hitChance * 100.0) + "% out of " + chance.toDouble() + "%")
        return chance < (hitChance*100)
    }
    return true
}