package net.dodian.uber.game.content.combat

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.content.combat.CombatAttackResult

fun Client.canAttackNpc(npcId: Int): Boolean {
    if(npcId == 6610 && getAttackStyle() != 2) {
        send(SendMessage("This monster can only be harmed by magic."))
        return false
    }
    if(npcId == 4304 && getAttackStyle() != 1) {
        send(SendMessage("This monster can only be harmed by ranged."))
        return false
    }
    if(npcId == 4303 && getAttackStyle() != 0) {
        send(SendMessage("This monster can only be harmed by melee."))
        return false
    }

    if (!checkSlayerTask(npcId))
        return false

    if (!requireKey(1545, 1443, 289))
        return false

    if (!requireKey(1544, 4067, 950))
        return false

    if (!requireKey(1543, 3964, 2075))
        return false
    return true
}
fun Client.getAttackStyle() : Int {
    if (hasStaff() && (autocast_spellIndex >= 0 || magicId >= 0))
        return 2
    else if (!hasStaff() && magicId >= 0)
        return 2
    else if (usingBow)
        return 1
    return 0
}

fun Client.attackTarget(): CombatAttackResult? {
    var attackStyle = getAttackStyle()
    /* Do checks each tick for player + npc */
    if(target is Npc) {
        val npc = Server.npcManager.getNpc(target.slot)
        if (npc.currentHealth < 1 || isDeathSequenceActive() || !canAttackNpc(npc.id)) {
            resetAttack()
            return null
        }
    }
    if(target is Player) { //Pvp checks (duel and wilderness)
        val player = resolveCombatTargetPlayer(target.slot) ?: run {
            resetAttack()
            return null
        }
        if (duelFight && attackStyle == 0 && duelRule[1]) {
            send(SendMessage("Melee has been disabled for this duel!"))
            resetAttack()
            return null
        }
        if (duelFight && attackStyle == 1 && duelRule[0]) {
            send(SendMessage("Ranged has been disabled for this duel!"))
            resetAttack()
            return null
        }
        if (duelFight && attackStyle == 2 && duelRule[2]) {
            send(SendMessage("Magic has been disabled for this duel!"))
            resetAttack()
            return null
        }
        if (!(duelFight && duel_with == target.slot) && !Server.pking) {
            send(SendMessage("Pking has been disabled"))
            resetAttack()
            return null
        }
        if (!canAttack) {
            send(SendMessage("You cannot attack your oppenent yet!"))
            resetAttack()
            return null
        }
        if (!(duelFight && duel_with == target.slot) && (!player.inWildy() || !inWildy())) {
            send(SendMessage("You can't attack that player!"))
            resetAttack()
            return null
        }
    }
    /*int diff = Math.abs(castOnPlayer.determineCombatLevel() - client.determineCombatLevel());
if (!((castOnPlayer.inWildy() && diff <= client.wildyLevel && diff <= castOnPlayer.wildyLevel)
            || client.duelFight && client.duel_with == castOnPlayer.getSlot()) || !castOnPlayer.saveNeeded) {
    client.send(new SendMessage("You can't attack that player"));
    return;
}*/ //TODO: Fix wildy checks if we release wilderness!
    /* Style check to attack! */
    if(attackStyle == 2)
        return handleMagicAttack()
    if(attackStyle == 1)
        return handleRangedAttack()
    if(attackStyle == 0)
        return handleMeleeAttack()
    return null
}
