package net.dodian.uber.game.combat

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.utilities.Utils
import kotlin.math.min

fun Client.handleMagic(): Int {
    val staves = listOf(2415, 2416, 2417, 4675, 4710, 6914)
    if (equipment[Equipment.Slot.WEAPON.id] !in staves || autocast_spellIndex < 0)
        return -1
    if (!canReach(target, 5))
        return 0


    val time = System.currentTimeMillis()
    val slot = autocast_spellIndex%4;
    if (time - lastAttack > coolDown[slot]) {
        isInCombat = true
        lastCombat = System.currentTimeMillis()
    } else return 0
    setFocus(target.position.x, target.position.y)
    if (getLevel(Skill.MAGIC) < requiredLevel[autocast_spellIndex]) {
        send(SendMessage("You need a magic level of ${requiredLevel[autocast_spellIndex]} to cast this spell!"))
        return 0
    }
    if (!runeCheck()) {
        resetAttack()
        return 0
    }
    if (target is Player && duelFight && duelRule[2]) {
        send(SendMessage("Magic has been disabled for this duel!"));
        resetAttack()
        return 0
    }
    deleteItem(565, 1)
    requestAnim(1979, 0)
    var maxHit = baseDamage[autocast_spellIndex] * magicDmg()
    if (target is Npc) { // Slayer damage!
        val npcId = Server.npcManager.getNpc(target.slot).id
        if(getSlayerDamage(npcId, true) == 2)
            maxHit * 1.2
    }
    var hit = Utils.random(maxHit.toInt())
    val criticalChance = getLevel(Skill.AGILITY) / 9
    val extra = getLevel(Skill.MAGIC) * 0.195
    val hitCrit = hit + Utils.dRandom2(extra).toInt()
    if(equipment[Equipment.Slot.SHIELD.id]==4224)
        criticalChance * 1.5
    val landCrit = Math.random() * 100 <= criticalChance
    if (target is Npc) {
        val npc = Server.npcManager.getNpc(target.slot)
        if (landCrit)
            hit + Utils.dRandom2(extra).toInt()
        if(hit >= npc.currentHealth)
            hit = npc.currentHealth
        if(slot == 2) { //Heal effect!
            currentHealth = min(getLevel(Skill.HITPOINTS), currentHealth + (hit / 3))
            refreshSkill(Skill.HITPOINTS)
        }
        npc.dealDamage(this, hit, landCrit)
    }
    if (target is Player) {
        val player = Server.playerHandler.getClient(target.slot)
        if (landCrit)
            hit + Utils.dRandom2(extra).toInt()
        if(hit >= player.currentHealth)
            hit = player.currentHealth
        if(slot == 2) { //Heal effect!
            currentHealth = min(getLevel(Skill.HITPOINTS), currentHealth + (hit / 3))
            refreshSkill(Skill.HITPOINTS)
        }
        player.dealDamage(hit, landCrit)
    }
    /* Magic graphics! */
    if (slot == 2) //Blood effect
        stillgfx(377, target.position.y, target.position.x)
    else if (slot == 3) //Freeze effect
        stillgfx(369, target.position.y, target.position.x)
    else //Other ancient effect!
        stillgfx(78, target.position.y, target.position.x)

    if(target is Npc) {
        giveExperience(40 * hit, Skill.MAGIC)
        giveExperience(15 * hit, Skill.HITPOINTS)
    }

    if (debug) send(SendMessage("hit = $hit, elapsed = ${time - lastAttack}"))
    resetWalkingQueue()
    lastAttack = System.currentTimeMillis()

    return 1
}