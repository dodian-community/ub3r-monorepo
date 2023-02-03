package net.dodian.uber.game.combat

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.utilities.Utils

fun Client.handleRanged(): Int {
    if (!usingBow) return -1
    if (!canReach(target, 5))
        return 0

    val time = System.currentTimeMillis()

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

    if (time - lastAttack > getbattleTimer(equipment[Equipment.Slot.WEAPON.id])) {
        isInCombat = true
        lastCombat = System.currentTimeMillis()
    } else return 0
    setFocus(target.position.x, target.position.y)
    if (target is Player && duelFight && duelRule[0]) {
        send(SendMessage("Ranged has been disabled for this duel!"));
        resetAttack()
        return 0
    }
    if (DeleteArrow()) {
        if(target is Npc) {
            val offsetX = (position.y - target.position.y) * 1
            val offsetY = (position.x - target.position.x) * 1
            sendAnimation(426)
            callGfxMask(arrowPullGfx, 100)
            arrowGfx(offsetY, offsetX, 50, 90, arrowGfx, 43, 35, target.slot + 1, 51, 16)
        } else {
            val offsetX = (position.y - target.position.y) * -1
            val offsetY = (position.x - target.position.x) * -1
            sendAnimation(426)
            callGfxMask(arrowPullGfx, 100)
            arrowGfx(offsetY, offsetX, 50, 90, arrowGfx, 43, 35, -(target.slot + 1), 51, 16)
        }
    } else {
        resetAttack()
        send(SendMessage("You're out of arrows!"))
        return 0
    }
    var maxHit = rangedMaxHit()
    if (target is Npc) { // Slayer damage!
        val npcId = Server.npcManager.getNpc(target.slot).id
        if(getSlayerDamage(npcId, true) == 2)
            maxHit * 1.2
    }
    var hit = Utils.random(maxHit)
    val criticalChance = getLevel(Skill.AGILITY) / 9
    if(equipment[Equipment.Slot.SHIELD.id]==4224)
        criticalChance * 1.5
    val extra = getLevel(Skill.RANGED) * 0.195
    val hitCrit = hit + Utils.dRandom2(extra).toInt()
    val landCrit = Math.random() * 100 <= criticalChance
    var landHit = landHitRanged(this, target);
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

    if (debug) send(SendMessage("hit = $hit, elapsed = ${time - lastAttack}"))
    resetWalkingQueue()
    lastAttack = System.currentTimeMillis()

    return 1
}

fun landHitRanged(p: Client, t: Entity): Boolean {
    var maxChance = 80.0
    var minChance = 20.0
    var hitChance = 60.0
    var chance = Math.random() * 100;
    if(t is Client) { //Pvp
        var atkBonus = p.playerBonus[3]
        var atkLevel = p.getLevel(Skill.RANGED) + (atkBonus / 10)
        var defBonus = t.playerBonus[8]
        var defLevel = t.getLevel(Skill.DEFENCE) + (defBonus / 10)
        hitChance += (atkLevel - defLevel)
        if(hitChance < minChance)
            hitChance = minChance;
        if(hitChance > maxChance)
            hitChance = maxChance;
        return chance < hitChance;
    } else if(t is Npc) { //Pve
        var atkBonus = p.playerBonus[3]
        var atkLevel = p.getLevel(Skill.RANGED) + (atkBonus / 10)
        var defLevel = t.defence
        hitChance += (atkLevel - defLevel)
        if(hitChance < minChance)
            hitChance = minChance;
        if(hitChance > maxChance)
            hitChance = maxChance;
        return chance < hitChance;
    }
    return true;
}