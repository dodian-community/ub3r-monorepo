package net.dodian.uber.game.combat

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerCore as Player
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.engine.systems.skills.asSkillPlayer
import net.dodian.uber.skills.slayer.SlayerCombatService
import net.dodian.uber.skills.prayer.PrayerCombatService
import net.dodian.utilities.Utils

fun Client.distance(entity: Entity) = Utils.getDistance(position.x, position.y, entity.position.x, entity.position.y)
fun Client.canReach(entity: Entity, distance: Int) = distance(entity) <= distance

fun Client.requireKey(keyId: Int, vararg npcId: Int): Boolean {
    if(target is Client) return true //No player check!
    if (!checkItem(keyId) && getPositionName(target.position) == Player.positions.KEYDUNG && Server.npcManager.getNpc(target.slot).id in npcId) {
        resetPos()
        resetAttack()
        return false
    }
    return true
}

fun Client.slayerLevelRequired(npcId: Int): Boolean {
    val denial = SlayerCombatService.canAttack(asSkillPlayer(), npcId, false) ?: return true
    send(SendMessage(denial)); return false
}

fun Client.checkSlayerTask(npcId: Int): Boolean {
    val mummyException = npcId == 950 && getPositionName(position) == Player.positions.KEYDUNG
    val denial = SlayerCombatService.canAttack(asSkillPlayer(), npcId, mummyException)
    if (denial != null) {
        send(SendMessage(denial))
        resetAttack()
        return false
    }
    return true
}

fun Client.magicBonusDamage(): Double {
    return magicDmg() + PrayerCombatService.magicDamageBonus(asSkillPlayer())
}

fun Client.meleeMaxHit(): Int {
    val prayerBonus = PrayerCombatService.meleeStrengthBonus(asSkillPlayer())
    val voidBonus = 0.0 // TODO: Probably not relevant for Dodian, at least not for a while
    var specialBonus = 0.0
    if(checkObsidianWeapons()) //Obsidian weapon should give 20% increase damage!
        specialBonus += 0.2
    if(armourSet("dharok"))
        specialBonus += (getMaxHealth() - getCurrentHealth()) / 100.0
    val styleBonus = when (fightType) {
        2 -> 3 // Aggressive
        3 -> 1 // Controlled
        else -> 0
    }
    val strengthBonus = playerBonus[10]
    val strength = getLevel(Skill.STRENGTH)
    val effectiveStrength = ((strength * (1 + prayerBonus)) + styleBonus + 8) * (1 + voidBonus)
    val baseDamage = 0.5 + effectiveStrength * (strengthBonus + 64) / 640

    return (baseDamage * (1 + specialBonus)).toInt()
}

fun Client.rangedMaxHit(): Int {
    val prayerBonus = PrayerCombatService.rangedStrengthBonus(asSkillPlayer())
    val voidBonus = 0.0 // TODO: Probably not relevant for Dodian, at least not for a while
    val specialBonus = 0.0 // TODO: Calculate special bonus

    val styleBonus = when (fightType) {
        0 -> 3 // Accurate
        else -> 0
    }
    val ranged = getLevel(Skill.RANGED)
    val effectiveStrength = ((ranged * (1 + prayerBonus)) + styleBonus + 8) * (1 + voidBonus)
    val baseDamage = 0.5 + (effectiveStrength * (getRangedStr() + 64) / 640)
    return (baseDamage * (1 + specialBonus)).toInt()
}

fun Client.getRangedStr(): Int {
    return playerBonus[11]
}

fun Client.getSlayerDamage(npcId: Int, range: Boolean): Int {
    return SlayerCombatService.damageBonus(asSkillPlayer(), npcId, range)
}
