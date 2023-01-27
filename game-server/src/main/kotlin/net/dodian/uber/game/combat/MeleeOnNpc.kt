package net.dodian.uber.game.combat

import net.dodian.uber.game.Server
import net.dodian.uber.game.combat.criticalHit
import net.dodian.uber.game.combat.canReach
import net.dodian.uber.game.model.UpdateFlag
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.item.SpecialsHandler
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.utilities.Range
import net.dodian.utilities.Utils
import kotlin.random.Random

fun Client.handleMelee(): Int {
    if (!canReach(selectedNpc, 1))
        return 0

    val time = System.currentTimeMillis()

        if (time - lastAttack > getbattleTimer(equipment[Equipment.Slot.WEAPON.id])) {
            isInCombat = true
            lastCombat = System.currentTimeMillis()
        } else return 0

        val emote = Server.itemManager.getAttackAnim(equipment[Equipment.Slot.WEAPON.id])
        setFocus(selectedNpc.position.x, selectedNpc.position.y)
        sendAnimation(emote)
        //updateFlags.setRequired(UpdateFlag.APPEARANCE, true)

        val hit = Utils.random(meleeMaxHit())
        val criticalHit = criticalHit(Skill.STRENGTH)
        val criticalDamageBonus = Random.nextInt(criticalHit.min, criticalHit.max)
        if (Math.random() <= criticalHit.chance)
            selectedNpc.dealDamage(this, hit + criticalDamageBonus, true)
        else selectedNpc.dealDamage(this, hit, false)

        if (FightType == 3) {
            val xp = (15 * hit) * CombatExpRate
            giveExperience(xp, Skill.ATTACK)
            giveExperience(xp, Skill.DEFENCE)
            giveExperience(xp, Skill.STRENGTH)
        } else giveExperience((40 * hit) * CombatExpRate, Skill.getSkill(FightType))

        giveExperience((15 * hit) * CombatExpRate, Skill.HITPOINTS)
        if (debug) send(SendMessage("hit = $hit, elapsed = ${time - lastAttack}"))

        if (debug) send(SendMessage("hit = $hit, elapsed = ${time - lastAttack}"))

    lastAttack = System.currentTimeMillis()
    updateFlags.setRequired(UpdateFlag.APPEARANCE, true)

    return 1
}

fun Client.handleSpecial(hit: Int): Int {
    var newHit = hit
    val emote = Server.itemManager.getAttackAnim(equipment[Equipment.Slot.WEAPON.id])
    if (selectedNpc.isAlive) {
        val chance = Range(1, 8).value
        if (chance == 1 && specsOn) {
            when (equipment[Equipment.Slot.WEAPON.id]) {
                4151, 7158 -> {
                    SpecialsHandler.specAction(this, equipment[Equipment.Slot.WEAPON.id], hitDiff)
                    newHit += bonusSpec
                    requestAnim(emoteSpec, 0)
                    // TODO: Uhm? Why y then x?
                    animation(animationSpec, selectedNpc.position.y, selectedNpc.position.x)
                }
            }
        } else requestAnim(emote, 0)
    } else resetAttackNpc()

    return newHit
}