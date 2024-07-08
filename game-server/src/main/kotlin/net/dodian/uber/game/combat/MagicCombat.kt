package net.dodian.uber.game.combat

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.network.packets.outgoing.SendMessage
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.prayer.Prayers
import net.dodian.utilities.Misc
import net.dodian.utilities.Utils
import kotlin.math.min

fun Client.handleMagicAttack(): Int {
    val slot = autocast_spellIndex%4
    if (combatTimer > 0 || stunTimer > 0) //Need this to be a check here!
        return 0
    if (target is Player && duelFight && duelRule[2]) {
        send(SendMessage("Magic has been disabled for this duel!"))
        resetAttack()
        return 0
    }
    if (getLevel(Skill.MAGIC) < requiredLevel[autocast_spellIndex]) {
        send(SendMessage("You need a magic level of ${requiredLevel[autocast_spellIndex]} to cast this spell!"))
        resetAttack()
        return 0
    }
    if (!runeCheck()) {
        resetAttack()
        return 0
    }

    combatTimer = coolDown[slot]
    lastCombat = 16
    setFocus(target.position.x, target.position.y)
    deleteItem(565, 1)
    checkItemUpdate()
    requestAnim(-1, 0)
    sendAnimation(1979)
    var maxHit = baseDamage[autocast_spellIndex] * magicBonusDamage()
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
    when (slot) {
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
        if(hit >= npc.currentHealth) hit = npc.currentHealth
        npc.dealDamage(this, hit, if(landCrit) Entity.hitType.CRIT else Entity.hitType.STANDARD)

        val chance = Misc.chance(8) == 1 && armourSet("ahrim")
        if(chance && hit > 0) { //Ahrim effect!
            stillgfx(400, npc.position, 100)
            heal(hit / 2)
        } else if(slot == 2) //Heal effect!
            heal(hit / 3)
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
        if(chance && hit > 0) { //Ahrim effect!
            stillgfx(400, player.position, 100)
            heal(hit / 2)
        } else if(slot == 2) //Heal effect!
            heal(hit / 3)
    }

    if (debug) send(SendMessage("hit = $hit, elapsed = $combatTimer"))

    return 1
}