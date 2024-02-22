package net.dodian.uber.game.combat

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.prayer.Prayer
import net.dodian.uber.game.model.player.skills.prayer.Prayers
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
        2266 -> if(getLevel(Skill.SLAYER) < 86) {
            send(SendMessage("You need a slayer level of 86 to harm this monster."))
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

fun Client.magicBonusDamage(): Double {
    val prayerBonus = if(prayerManager.isPrayerOn(Prayers.Prayer.MYSTIC_WILL)) 0.05
    else if(prayerManager.isPrayerOn(Prayers.Prayer.MYSTIC_LORE)) 0.1
    else if(prayerManager.isPrayerOn(Prayers.Prayer.MYSTIC_MIGHT)) 0.15
    else 0.0
    return magicDmg() + prayerBonus
}

fun Client.meleeMaxHit(): Int {
    val prayerBonus = if(prayerManager.isPrayerOn(Prayers.Prayer.BURST_OF_STRENGTH)) 0.05
    else if(prayerManager.isPrayerOn(Prayers.Prayer.SUPERHUMAN_STRENGTH)) 0.1
    else if(prayerManager.isPrayerOn(Prayers.Prayer.ULTIMATE_STRENGTH)) 0.15
    else if(prayerManager.isPrayerOn(Prayers.Prayer.CHIVALRY)) 0.18
    else if(prayerManager.isPrayerOn(Prayers.Prayer.PIETY)) 0.22
    else 0.0
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
    val effectiveStrength = ((strength * (1 + prayerBonus)) + styleBonus + 8) * (1 + voidBonus)
    val baseDamage = 0.5 + effectiveStrength * (strengthBonus + 64) / 640

    return (baseDamage * (1 + specialBonus)).toInt()
}

fun Client.rangedMaxHit(): Int {
    val prayerBonus = if(prayerManager.isPrayerOn(Prayers.Prayer.SHARP_EYE)) 0.05
    else if(prayerManager.isPrayerOn(Prayers.Prayer.HAWK_EYE)) 0.1
    else if(prayerManager.isPrayerOn(Prayers.Prayer.EAGLE_EYE)) 0.15
    else 0.0
    val voidBonus = 0.0 // TODO: Probably not relevant for Dodian, at least not for a while
    val specialBonus = 0.0 // TODO: Calculate special bonus

    val styleBonus = when (FightType) {
        2 -> 3 // Rapid
        0, 3 -> 0 // Accuracy and Long range
        else -> error("Fight style ID '$FightType' was unexpected!")
    }
    val ranged = getLevel(Skill.RANGED)
    val effectiveStrength = ((ranged * (1 + prayerBonus)) + styleBonus + 8) * (1 + voidBonus)
    val baseDamage = 0.5 + (effectiveStrength * (getRangedStr() + 64) / 640)
    return (baseDamage * (1 + specialBonus)).toInt()
}

fun Client.getRangedStr(): Int {
    var rangedStr = when(equipment[Equipment.Slot.ARROWS.id]) {
        882 -> 7
        884 -> 10
        886 -> 16
        888 -> 22
        890 -> 31
        892 -> 49
        11212 -> 60
        else -> 0
    }
    rangedStr += when(equipment[Equipment.Slot.WEAPON.id]) {
        843 -> 10
        845 -> 15
        849 -> 15
        847 -> 20
        853 -> 20
        851 -> 25
        857 -> 25
        855 -> 30
        861 -> 30
        859 -> 35
        4212 -> 68
        6724 -> 78
        20997 -> 88
        else -> 0
    }
    /* Head */
    rangedStr += when(equipment[Equipment.Slot.HEAD.id]) {
        1169 -> 1
        2581 -> 3
        6131 -> 3
        else -> 0
    }
    /* Body */
    rangedStr += when(equipment[Equipment.Slot.CHEST.id]) {
        1129 -> 1
        1743 -> 2
        1135 -> 5
        2499 -> 7
        2501 -> 9
        2503 -> 11
        6133 -> 13
        else -> 0
    }
    /* Legs */
    rangedStr += when(equipment[Equipment.Slot.LEGS.id]) {
        1095 -> 1
        1097 -> 2
        1099 -> 4
        2493 -> 5
        2495 -> 6
        2497 -> 7
        6135 -> 9
        else -> 0
    }
    /* Boots */
    rangedStr += when(equipment[Equipment.Slot.FEET.id]) {
        2577 -> 3
        6143 -> 3
        else -> 0
    }
    /* Hands */
    rangedStr += when(equipment[Equipment.Slot.HANDS.id]) {
        1065 -> 1
        2487 -> 2
        2489 -> 3
        2491 -> 4
        6149 -> 4
        else -> 0
    }
    /* Shield */
    rangedStr += when(equipment[Equipment.Slot.SHIELD.id]) {
        3844 -> 9
        else -> 0
    }
    /* Blessing */
    rangedStr += when(equipment[Equipment.Slot.BLESSING.id]) {
        20226 -> 9
        20229 -> 18
        else -> 0
    }
    /* Ring */
    rangedStr += when(equipment[Equipment.Slot.RING.id]) {
        773 -> 1000
        4202 -> 4
        else -> 0
    }
    return rangedStr
}

fun Client.getSlayerDamage(npcId: Int, range: Boolean): Int {
    if(!range && blackMaskEffect(npcId))
        return 1
    else if(blackMaskImbueEffect(npcId) || (range && blackMaskImbueEffect(npcId)))
        return 2
    return 0
}