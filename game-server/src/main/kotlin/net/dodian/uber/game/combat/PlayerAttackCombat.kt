package net.dodian.uber.game.combat

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.network.packets.outgoing.SendMessage

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
    if (hasStaff() && (autocast_spellIndex >= 0 || magicId != -1))
        return 2
    if (!hasStaff() && magicId != -1)
        return 2
    if (usingBow)
        return 1
    return 0
}

fun Client.attackTarget(): Boolean {
    if (target is Npc) {
        val npc = Server.npcManager.getNpc(target.slot)
        if (npc.currentHealth < 1 || deathTimer > 0 || !canAttackNpc(npc.id)) {
            resetAttack()
            return false
        }
    }
    if (target is Player) {
        val plr = Server.playerHandler.getClient(target.slot)
        if (plr.currentHealth < 1 || deathTimer > 0) {
            resetAttack()
            return false
        }
    }
    if(getAttackStyle() == 2 && handleMagicAttack() == 1)
        return true
    if(getAttackStyle() == 1 && handleRangedAttack() == 1)
        return true
    if(getAttackStyle() == 0 && handleMeleeAttack() == 1)
        return true
    return false
}
