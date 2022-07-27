package net.dodian.uber.game.combat

import net.dodian.uber.game.combat.extensions.checkSlayerTask
import net.dodian.uber.game.combat.extensions.requireKey
import net.dodian.uber.game.combat.magic.handleMagic
import net.dodian.uber.game.combat.melee.handleMelee
import net.dodian.uber.game.combat.ranged.handleRanged
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage

fun Client.attackNpc(): Boolean {
    val npcId = selectedNpc.id

    if (selectedNpc.currentHealth < 1 || deathTimer > 0) {
        resetAttackNpc()
        return false
    }

    if (!checkSlayerTask(npcId))
        return false

    if (npcId == 4130 && determineCombatLevel() < 50) {
        send(SendMessage("You must be level 50 combat or higher to attack Dad!"))
        resetAttackNpc()
        return false
    }

    if (!requireKey(1545, 1443, 289))
        return false

    if (!requireKey(1544, 4067, 950))
        return false

    if (!requireKey(1543, 3964, 2075))
        return false

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
