package net.dodian.uber.game.combat

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player

fun Client.canAttackNpc(npcId: Int): Boolean {
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
        if (plr == null || plr.currentHealth < 1 || deathTimer > 0) {
            resetAttack()
            return false
        }
    }

    when (handleMagic()) {
        0 -> return false
        1 -> return true
    }

    when (handleRanged()) {
        0 -> return false
        1 -> return true
    }

    when (handleMelee()) {
        0 -> return false
        1 -> return true
    }

    return false
}
