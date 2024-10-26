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
import kotlin.math.min

fun Client.handleMagicAttack(): Int {
    if (combatTimer > 0 || stunTimer > 0 || target == null) //Need this to be a check here!
        return 0
    if(goodDistanceEntity(target, 5))
        resetWalkingQueue()

    var slot = autocast_spellIndex
    var type = 0
    if(slot >= 0 && magicId < 0)
        type = autocast_spellIndex%4
    else {
        for(checkSlot in 1..ancientId.size)
            if(magicId == ancientId[checkSlot]) {
                slot = checkSlot
                type = checkSlot%4
                break
            }
    }
    /* Checks after known magic cast! */
    if (getLevel(Skill.MAGIC) < requiredLevel[slot]) {
        send(SendMessage("You need a magic level of ${requiredLevel[slot]} to cast this spell!"))
        resetAttack()
        return 0
    }
    if (!runeCheck()) {
        resetAttack()
        return 0
    }

    combatTimer = coolDown[type]
    lastCombat = 16
    setFocus(target.position.x, target.position.y)
    deleteItem(565, 1)
    checkItemUpdate()
    sendAnimation(1979)
    var maxHit = baseDamage[slot] * magicBonusDamage()
    if (target is Npc) { // Slayer damage!
        val checkNpc = Server.npcManager.getNpc(target.slot)
        if(getSlayerDamage(checkNpc.id, true) == 2)
            maxHit *= 1.2
        if(checkNpc.boss) {
            val reduceDefence = min(checkNpc.defence / 15, 18)
            val value = (12.0 + Misc.random(reduceDefence)) / 100.0
            maxHit *= 1.0 - value
            //System.out.println("reduce value: $value and defence $reduceDefence to be new max hit $maxHit")
        }
    }
    var hit = Utils.random(maxHit.toInt())
    val criticalChance = getLevel(Skill.AGILITY) / 9
    val extra = getLevel(Skill.MAGIC) * 0.195
    if(equipment[Equipment.Slot.SHIELD.id]==4224) criticalChance * 1.5
    val landCrit = Math.random() * 100 <= criticalChance
    /* Magic graphics! */
    when (type) {
        0 //Burn effect!
        -> stillgfx(357, target.position.y, target.position.x)
        1 //Shadow effect, poison?!
        -> stillgfx(379, target.position.y, target.position.x)
        2 //Blood effect
        -> stillgfx(377, target.position.y, target.position.x)
        3 //Freeze effect
        -> stillgfx(369, target.position.y, target.position.x)
        else //Other ancient effect!
        -> stillgfx(78, target.position.y, target.position.x)
    }
    if (target is Npc) {
        val npc = Server.npcManager.getNpc(target.slot)
        if (landCrit) hit + Utils.dRandom2(extra).toInt()
        if (hit >= npc.currentHealth) hit = npc.currentHealth
        npc.dealDamage(this, hit, if (landCrit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
        val chance = Misc.chance(8) == 1 && armourSet("ahrim")
        /* Ancient effects */
        if(type == 2) { //Heal effect
            if(!chance)
                heal(hit / 3)
            else if(hit > 0) { //Burn effect
                stillgfx(400, npc.position, 100)
                heal(hit / 2)
            }
        } else if (type == 0 && hit > 0) {
            if((Misc.chance(6) == 1) || (armourSet("ahrim") && Misc.chance(3) == 1)) //Do burn!
                npc.inflictEffect(1, true, getSlot(), slot/4 + 1, 5)
        }
        /* Give experience */
        giveExperience(40 * hit, Skill.MAGIC)
        giveExperience(13 * hit, Skill.HITPOINTS)
    }
    if (target is Player) {
        val player = Server.playerHandler.getClient(target.slot)
        if (landCrit) hit + Utils.dRandom2(extra).toInt()
        if (player.prayerManager.isPrayerOn(Prayers.Prayer.PROTECT_MAGIC)) (hit * 0.6).toInt()
        if(hit >= player.currentHealth) hit = player.currentHealth
        player.dealDamage(this, hit, if(landCrit) Entity.hitType.CRIT else Entity.hitType.STANDARD)
        val chance = Misc.chance(8) == 1 && armourSet("ahrim")
        /* Ancient effects */
        if(type == 2) { //Heal effect
            if(!chance)
                heal(hit / 3)
            else if(hit > 0) {
                stillgfx(400, player.position, 100)
                heal(hit / 2)
            }
        } else if (type == 0) { //Burn effect
            /*if((Misc.chance(6) == 1) || (armourSet("ahrim") && Misc.chance(3) == 1)) //Do burn!
                System.out.println("BURN! " + (autocast_spellIndex/4 + 1)) */
        }
    }

    if(magicId >= 0) { //Set this because auto magicId should be set to -1 after one cast!
        magicId = -1
        if(autocast_spellIndex < 0) target = null //Set this as no target if we got no autocast set!
    }

    if (debug) send(SendMessage("hit = $hit, elapsed = $combatTimer"))

    return 1
}