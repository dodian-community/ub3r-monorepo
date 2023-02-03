package net.dodian.uber.game.combat

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.item.SpecialsHandler
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.utilities.Range
import net.dodian.utilities.Utils

fun Client.handleMelee(): Int {
    if (!canReach(target, 1))
        return 0

    val time = System.currentTimeMillis()

        if (time - lastAttack > getbattleTimer(equipment[Equipment.Slot.WEAPON.id])) {
            isInCombat = true
            lastCombat = System.currentTimeMillis()
        } else return 0

        val emote = Server.itemManager.getAttackAnim(equipment[Equipment.Slot.WEAPON.id])
        setFocus(target.position.x, target.position.y)
        if (target is Player && duelFight && duelRule[1]) {
            send(SendMessage("Melee has been disabled for this duel!"));
            resetAttack()
            return 0
        }
        sendAnimation(emote)
        var maxHit = meleeMaxHit()
         if (target is Npc) { // Slayer damage!
             val npcId = Server.npcManager.getNpc(target.slot).id
             if(getSlayerDamage(npcId, false) == 1)
                 maxHit * 1.15
             else if(getSlayerDamage(npcId, false) == 2)
                 maxHit * 1.2
         }
        var hit = Utils.random(maxHit)
        val criticalChance = getLevel(Skill.AGILITY) / 9
        if(equipment[Equipment.Slot.SHIELD.id]==4224)
            criticalChance * 1.5
        val extra = getLevel(Skill.STRENGTH) * 0.195
        val hitCrit = hit + Utils.dRandom2(extra).toInt()
        val landCrit = Math.random() * 100 <= criticalChance
        var landHit = landHit(this, target);
        if (target is Npc) {
            val npc = Server.npcManager.getNpc(target.slot)
            if (landCrit && landHit)
                hit + Utils.dRandom2(extra).toInt()
            else if(!landHit) hit = 0
            if(hit >= npc.currentHealth)
                hit = npc.currentHealth
            npc.dealDamage(this, hit, landCrit && landHit)
        }
        if (target is Player) {
            val player = Server.playerHandler.getClient(target.slot)
            if (landCrit && landHit)
                hit + Utils.dRandom2(extra).toInt()
            else if(!landHit) hit = 0
            if(hit >= player.currentHealth)
                hit = player.currentHealth
            player.dealDamage(hit, landCrit && landHit)
        }

        if (target is Npc) {
            if (FightType == 3) {
                val xp = (15 * hit) * CombatExpRate
                giveExperience(xp, Skill.ATTACK)
                giveExperience(xp, Skill.DEFENCE)
                giveExperience(xp, Skill.STRENGTH)
            } else giveExperience((40 * hit) * CombatExpRate, Skill.getSkill(FightType))

            giveExperience((15 * hit) * CombatExpRate, Skill.HITPOINTS)
        }
        if (debug) send(SendMessage("hit = $hit, elapsed = ${time - lastAttack}"))
    lastAttack = System.currentTimeMillis()

    return 1
}

fun highestAttackBonus(p: Client): Int {
    var bonus = 0;
    for (i in 0..2) {
        if (p.playerBonus[i] > bonus)
            bonus = p.playerBonus[i]
    }
    return bonus;
    }
    fun highestDefensiveBonus(p: Client): Int {
        var bonus = 0;
        for (i in 5..7) {
            if (p.playerBonus[i] > bonus)
                bonus = p.playerBonus[i]
        }
            return bonus;
        }
fun landHit(p: Client, t: Entity): Boolean {
    var maxChance = 80.0
    var minChance = 20.0
    var hitChance = 60.0
    var chance = Math.random() * 100;
    if(t is Client) { //Pvp
        var atkBonus = highestAttackBonus(p)
        var atkLevel = p.getLevel(Skill.ATTACK) + (atkBonus / 10)
        var defBonus = highestDefensiveBonus(t)
        var defLevel = t.getLevel(Skill.DEFENCE) + (defBonus / 10)
        hitChance += (atkLevel - defLevel)
        if(hitChance < minChance)
            hitChance = minChance;
        if(hitChance > maxChance)
            hitChance = maxChance;
        return chance < hitChance;
    } else if(t is Npc) { //Pve
        var atkBonus = highestAttackBonus(p)
        var atkLevel = p.getLevel(Skill.ATTACK) + (atkBonus / 10)
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

fun Client.handleSpecial(hit: Int): Int {
    var newHit = hit
    val emote = Server.itemManager.getAttackAnim(equipment[Equipment.Slot.WEAPON.id])
    if (selectedNpc.isAlive) {
        val chance = Range(1, 8).value
        if (chance == 1 && specsOn) {
            when (equipment[Equipment.Slot.WEAPON.id]) {
                4151, 7158 -> {
                    SpecialsHandler.specAction(this, equipment[Equipment.Slot.WEAPON.id], hitDiff)
                    newHit += bonusSpec
                    requestAnim(emoteSpec, 0)
                    // TODO: Uhm? Why y then x?
                    animation(animationSpec, selectedNpc.position.y, selectedNpc.position.x)
                }
            }
        } else requestAnim(emote, 0)
    } else resetAttack()

    return newHit
}