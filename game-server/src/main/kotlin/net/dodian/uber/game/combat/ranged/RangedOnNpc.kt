package net.dodian.uber.game.combat.ranged

import net.dodian.uber.game.combat.extensions.distance
import net.dodian.uber.game.model.UpdateFlag
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.utilities.Utils

fun Client.handleRanged(): Int {
    if (!usingBow) return -1

    val time = System.currentTimeMillis()

    val arrows = mapOf(
        882 to listOf(10, 19),
        884 to listOf(9, 18),
        886 to listOf(11, 20),
        888 to listOf(12, 21),
        890 to listOf(13, 22),
        892 to listOf(14, 23),
        11212 to listOf(1120, 1116)
    )

    val equippedArrow = equipment[Equipment.Slot.ARROWS.id]
    val arrowGfx = arrows[equippedArrow]?.get(0) ?: 10
    val arrowPullGfx = arrows[equippedArrow]?.get(1) ?: 20
    val targetSlot = selectedNpc.slot

    if (distance(selectedNpc) > 5)
        return 0

    if (time - lastAttack > getbattleTimer(equipment[Equipment.Slot.WEAPON.id])) {
        isInCombat = true
        lastCombat = System.currentTimeMillis()
    } else return 0
    setFocus(selectedNpc.position.x, selectedNpc.position.y)
    if (DeleteArrow()) {
        // TODO: What's going on here?
        val offsetX = (position.y - selectedNpc.position.y) * 1
        val offsetY = (position.x - selectedNpc.position.x) * 1
        sendAnimation(426)
        callGfxMask(arrowPullGfx, 100)
        arrowGfx(offsetY, offsetX, 50, 90, arrowGfx, 43, 35, targetSlot + 1, 51, 16)
        setFocus(selectedNpc.position.x, selectedNpc.position.y)
        updateFlags.setRequired(UpdateFlag.APPEARANCE, true)
    } else {
        resetAttackNpc()
        send(SendMessage("You're out of arrows!"))
        return 0
    }
    updateFlags.setRequired(UpdateFlag.APPEARANCE, true)
    // TODO: Reimplement the critical stuff...
    val criticalChance = getLevel(Skill.AGILITY) / 9
    val extra = getLevel(if (usingBow) Skill.RANGED else Skill.STRENGTH) * 0.195

    val hit = Utils.random(rangedMaxHit())
    if (Math.random() * 100 <= criticalChance)
        selectedNpc.dealDamage(this, (hit + Utils.dRandom2(extra)).toInt(), true)
    else selectedNpc.dealDamage(this, hit, false)

    val xp = (if (FightType != 3) 40 * hit else 20 * hit) * CombatExpRate
    giveExperience(xp, Skill.RANGED)
    if (FightType == 3) giveExperience(xp, Skill.DEFENCE)

    giveExperience((15 * hit) * CombatExpRate, Skill.HITPOINTS)

    if (debug) send(SendMessage("hit = $hit, elapsed = ${time - lastAttack}"))

    lastAttack = System.currentTimeMillis()

    return 1
}