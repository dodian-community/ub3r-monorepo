package net.dodian.uber.game.combat

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.slayer.SlayerTask
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
    when (npcId) {
        2266 -> if(getLevel(Skill.SLAYER) < 90) {
            send(SendMessage("You need a slayer level of 90 to harm this monster."))
            return false
        }
        3209 -> if(getLevel(Skill.SLAYER) < 65) {
            send(SendMessage("You need a slayer level of 65 to harm this monster."))
            return false
        }
        3204 -> if(getLevel(Skill.SLAYER) < 45) {
            send(SendMessage("You need a slayer level of 45 to harm this monster."))
            return false
        }
        3201 -> if(getLevel(Skill.SLAYER) < 25) {
            send(SendMessage("You need a slayer level of 25 to harm this monster."))
            return false
        }
    }
    return true
}

fun Client.checkSlayerTask(npcId: Int): Boolean {
    val slayerTask = SlayerTask.slayerTasks.getSlayerNpc(npcId)
    val slayExceptions = (slayerTask == null)
            || (slayerTask == SlayerTask.slayerTasks.MUMMY && getPositionName(position) == Player.positions.KEYDUNG)

    if (!slayExceptions && slayerTask.isSlayerOnly && (slayerTask.ordinal != slayerData[1] || slayerData[3] <= 0)) {
        send(SendMessage("You need a slayer task to kill this monster."))
        resetAttack()
        return false
    }

    if(!slayerLevelRequired(npcId)) {
        resetAttack()
        return false
    }

    return true
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Client.meleeMaxHit(): Int {
    val potionBonus = 0.0 // TODO: Calculate potion bonus
    val prayerBonus = 0.0 // TODO: Implement prayer? and calculate bonus?
    val voidBonus = 0.0 // TODO: Probably not relevant for Dodian, at least not for a while
    val specialBonus = 0.0 // TODO: Calculate special bonus

    val styleBonus = when (FightType) {
        0, 1 -> 0 // Accurate & Defensive
        2 -> 3 // Aggressive
        3 -> 1 // Controlled
        else -> error("Fight style ID '$FightType' was unexpected!")
    }
    val strengthBonus = playerBonus[10]

    val strength = getLevel(Skill.STRENGTH)
    val effectiveStrength = (((strength + (1 + potionBonus)) * (1 + prayerBonus)) + styleBonus + 8) * (1 + voidBonus)
    val baseDamage = 0.5 + effectiveStrength * (strengthBonus + 64) / 640

    return (baseDamage * (1 + specialBonus)).toInt()
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Client.rangedMaxHit(): Int {
    val potionBonus = 0.0 // TODO: Calculate potion bonus
    val prayerBonus = 0.0 // TODO: Implement prayer? and calculate bonus?
    val otherBonus = 0.0 // TODO: Not sure what this is
    val specialBonus = 0.0 // TODO: Calculate special bonus

    val styleBonus = when (FightType) {
        1 -> 3 // Accurate
        2, 3 -> 0 // Long Range & Rapid
        else -> error("Fight style ID '$FightType' was unexpected!")
    }
    val rangedBonus = playerBonus[4]

    val ranged = getLevel(Skill.STRENGTH)
    val effectiveStrength = ((ranged + (1 + potionBonus)) * (1 + prayerBonus) * (1 + otherBonus)) + styleBonus
    val baseDamage = 1.3 + (effectiveStrength / 10) + (rangedBonus / 80) + ((effectiveStrength * ranged) / 640)

    return (baseDamage * (1 + specialBonus)).toInt()
}

fun Client.getSlayerDamage(npcId: Int, range: Boolean): Int {
    if(!range && blackMaskEffect(npcId))
        return 1
    else if(blackMaskImbueEffect(npcId) || (range && blackMaskImbueEffect(npcId)))
        return 2
    return 0
}