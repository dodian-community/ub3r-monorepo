package net.dodian.uber.game.content.items.consumables

import net.dodian.uber.game.content.items.ItemContent
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.Skills
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.utilities.Utils

object PotionItems : ItemContent {
    override val itemIds: IntArray = intArrayOf(
        121, 123, 125, 2428, // Attack potion
        113, 115, 117, 119, // Strength potion
        133, 135, 137, 2432, // Defence potion
        145, 147, 149, 2436, // Super attack potion
        157, 159, 161, 2440, // Super strength potion
        163, 165, 167, 2442, // Super defence potion
        169, 171, 173, 2444, // Ranging potion
        139, 141, 143, 2434, // Prayer potion
        3026, 3028, 3030, 3024, // Super restore potion
        12695, 12697, 12699, 12701, // Super combat potion
        11730, 11731, 11732, 11733, // Overload
        2452, 2454, 2456, 2458, // Anti-fire potion
    )

    override fun onFirstClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        if (!client.playerHasItem(itemId)) {
            return false
        }

        val blockedByDeathOrDuel = client.deathStage > 0 || client.deathTimer > 0 || client.inDuel
        var nextId = -1

        when (itemId) {
            121, 123, 125, 2428 -> {
                if (blockedByDeathOrDuel) return true
                client.requestAnim(1327, 0)
                client.boost(3 + (levelFor(client, Skill.ATTACK) * 0.1).toInt(), Skill.ATTACK)
                nextId = nextDose(itemId)
                client.send(SendMessage(if (nextId == 229) "You empty the attack potion." else "You drink the attack potion."))
            }

            113, 115, 117, 119 -> {
                if (blockedByDeathOrDuel) return true
                client.requestAnim(1327, 0)
                client.boost(3 + (levelFor(client, Skill.STRENGTH) * 0.1).toInt(), Skill.STRENGTH)
                nextId = nextDose(itemId)
                client.send(SendMessage(if (nextId == 229) "You empty the strength potion." else "You drink the strength potion."))
            }

            133, 135, 137, 2432 -> {
                if (blockedByDeathOrDuel) return true
                client.requestAnim(1327, 0)
                client.boost(3 + (levelFor(client, Skill.DEFENCE) * 0.1).toInt(), Skill.DEFENCE)
                nextId = nextDose(itemId)
                client.send(SendMessage(if (nextId == 229) "You empty the defense potion." else "You drink the defense potion."))
            }

            145, 147, 149, 2436 -> {
                if (blockedByDeathOrDuel) return true
                client.requestAnim(1327, 0)
                client.boost(5 + (levelFor(client, Skill.ATTACK) * 0.15).toInt(), Skill.ATTACK)
                nextId = nextDose(itemId)
                client.send(SendMessage(if (nextId == 229) "You empty the super attack potion." else "You drink the super attack potion."))
            }

            157, 159, 161, 2440 -> {
                if (blockedByDeathOrDuel) return true
                client.requestAnim(1327, 0)
                client.boost(5 + (levelFor(client, Skill.STRENGTH) * 0.15).toInt(), Skill.STRENGTH)
                nextId = nextDose(itemId)
                client.send(SendMessage(if (nextId == 229) "You empty the super strength potion." else "You drink the super strength potion."))
            }

            163, 165, 167, 2442 -> {
                if (blockedByDeathOrDuel) return true
                client.requestAnim(1327, 0)
                client.boost(5 + (levelFor(client, Skill.DEFENCE) * 0.15).toInt(), Skill.DEFENCE)
                client.refreshSkill(Skill.DEFENCE)
                nextId = nextDose(itemId)
                client.send(SendMessage(if (nextId == 229) "You empty the super defense potion." else "You drink the super defense potion."))
            }

            169, 171, 173, 2444 -> {
                if (blockedByDeathOrDuel) return true
                client.requestAnim(1327, 0)
                client.boost(4 + (levelFor(client, Skill.RANGED) * 0.12).toInt(), Skill.RANGED)
                nextId = nextDose(itemId)
                client.send(SendMessage(if (nextId == 229) "You empty the ranging potion." else "You drink the ranging potion."))
            }

            139, 141, 143, 2434 -> {
                if (blockedByDeathOrDuel) return true
                client.requestAnim(1327, 0)
                client.pray(8 + (client.maxPrayer * 0.25).toInt())
                client.refreshSkill(Skill.PRAYER)
                nextId = nextDose(itemId)
                client.send(SendMessage(if (nextId == 229) "You empty the prayer potion." else "You drink the prayer potion."))
            }

            3026, 3028, 3030, 3024 -> {
                if (blockedByDeathOrDuel) return true
                client.requestAnim(1327, 0)
                client.pray(10 + (client.maxPrayer * 0.28).toInt())
                client.refreshSkill(Skill.PRAYER)
                nextId = nextDose(itemId)
                client.send(SendMessage(if (nextId == 229) "You empty the restore potion." else "You drink the restore potion."))
            }

            12695, 12697, 12699, 12701 -> {
                if (blockedByDeathOrDuel) return true
                client.requestAnim(1327, 0)
                for (skillIndex in 0..2) {
                    val skill = Skill.getSkill(skillIndex) ?: continue
                    client.boost(5 + (levelFor(client, skill) * 0.15).toInt(), skill)
                }
                nextId = nextDose(itemId)
                client.send(SendMessage(if (nextId == 229) "You empty the super combat potion." else "You drink the super combat potion."))
            }

            11730, 11731, 11732, 11733 -> {
                if (blockedByDeathOrDuel || client.currentHealth < 11) return true
                client.requestAnim(1327, 0)
                client.dealDamage(null, 10, Entity.hitType.CRIT)
                for (skillIndex in intArrayOf(0, 1, 2, 4)) {
                    val skill = Skill.getSkill(skillIndex) ?: continue
                    client.boost(5 + (levelFor(client, skill) * 0.15).toInt(), skill)
                }
                val ticks = (1 + levelFor(client, Skill.HERBLORE)) * 2
                client.addEffectTime(2, 200 + ticks)
                nextId = nextDose(itemId)
                client.send(SendMessage(if (nextId == 229) "You empty the overload potion." else "You drink the overload potion."))
            }

            2452, 2454, 2456, 2458 -> {
                if ((client.effects.size > 1 && client.effects[1] > 50) || blockedByDeathOrDuel) return true
                client.requestAnim(1327, 0)
                client.addEffectTime(1, 500)
                nextId = nextDose(itemId)
                client.send(SendMessage(if (nextId == 229) "You empty the anti-fire potion." else "You drink the anti-fire potion."))
            }

            else -> return false
        }

        client.deleteItem(itemId, itemSlot, 1)
        if (nextId > 0) {
            client.addItemSlot(nextId, 1, itemSlot)
        }
        return true
    }

    private fun levelFor(client: Client, skill: Skill): Int {
        return Skills.getLevelForExperience(client.getExperience(skill))
    }

    private fun nextDose(itemId: Int): Int {
        for (index in Utils.pot_4_dose.indices) {
            when (itemId) {
                Utils.pot_4_dose[index] -> return Utils.pot_3_dose[index]
                Utils.pot_3_dose[index] -> return Utils.pot_2_dose[index]
                Utils.pot_2_dose[index] -> return Utils.pot_1_dose[index]
                Utils.pot_1_dose[index] -> return 229
            }
        }
        return -1
    }
}
