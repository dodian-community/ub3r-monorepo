package net.dodian.uber.game.combat

import net.dodian.uber.game.model.UpdateFlag
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.utilities.Utils
import kotlin.math.ceil
import kotlin.math.min


fun Client.handleMagic(): Int {
    val staves = listOf(2415, 2416, 2417, 4675, 4710, 6914)
    if (equipment[Equipment.Slot.WEAPON.id] !in staves || autocast_spellIndex < 0)
        return -1
    val slot = autocast_spellIndex%4;

    if (System.currentTimeMillis() - lastAttack < coolDown[slot])
        return 0

    isInCombat = true
    lastCombat = System.currentTimeMillis()
    lastAttack = System.currentTimeMillis()

    if (getLevel(Skill.MAGIC) < requiredLevel[autocast_spellIndex]) {
        send(SendMessage("You need a magic level of ${requiredLevel[autocast_spellIndex]} to cast this spell!"))
        return 0
    }

    if (!runeCheck()) {
        ResetAttack()
        return 0
    }

    deleteItem(565, 1)
    // TODO: Insert logging for rune being removed

    val damage = baseDamage[autocast_spellIndex] * magicDmg()
    val hit = min(Utils.random(damage.toInt()), selectedNpc.currentHealth)

    requestAnim(1979, 0)

    resetWalkingQueue()

    if (slot == 2) { //Blood effect
        stillgfx(377, selectedNpc.position.y, selectedNpc.position.x)
        currentHealth = min(getLevel(Skill.HITPOINTS), currentHealth + (hit / 5))
    } else if (slot == 3) { //Freeze effect
        stillgfx(369, selectedNpc.position.y, selectedNpc.position.x)
    } else { //Other ancient effect!
        animation(78, selectedNpc.position.y, selectedNpc.position.x)
    }

    setFocus(selectedNpc.position.x, selectedNpc.position.y)
    giveExperience(40 * hit, Skill.MAGIC)
    giveExperience(15 * hit, Skill.HITPOINTS)
    updateFlags.setRequired(UpdateFlag.APPEARANCE, true)
    selectedNpc.dealDamage(this, hit, false);
    // TODO: Implement critical hits?

    return 1
}