package net.dodian.uber.game.combat

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.model.item.Equipment

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
fun Client.getAttackStyle() : Int {
    val staves = listOf(2415, 2416, 2417, 4675, 4710, 6914, 6526)
    if (equipment[Equipment.Slot.WEAPON.id] in staves && autocast_spellIndex >= 0)
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
    if(getAttackStyle() == 2 && handleMagic() == 1)
        return true
    if(getAttackStyle() == 1 && handleRanged() == 1)
        return true
    if(getAttackStyle() == 0 && handleMelee() == 1)
        return true
    return false
}
