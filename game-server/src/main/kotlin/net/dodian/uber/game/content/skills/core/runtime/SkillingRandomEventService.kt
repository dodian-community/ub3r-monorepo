package net.dodian.uber.game.content.skills.core.runtime

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendString
import net.dodian.utilities.Misc
import net.dodian.utilities.Utils

object SkillingRandomEventService {
    @JvmStatic
    fun show(client: Client) {
        client.resetAction(true)
        if (client.randomed && client.randomed2) {
            return
        }
        client.random_skill = Utils.random(20)
        client.sendString("Click the @or1@${Skill.getSkill(client.random_skill)?.name ?: "skill"} @yel@button", 2810)
        client.sendString("", 2811)
        client.sendString("", 2831)
        client.randomed = true
        client.clearTabs()
        client.openInterface(2808)
    }

    @JvmStatic
    fun trigger(client: Client, awardedExperience: Int) {
        if (client.genieCombatFlag || client.randomed) {
            return
        }
        var xp = awardedExperience / 5
        var reduceChance =
            minOf(
                when {
                    xp < 50 -> xp
                    xp < 100 -> (xp * 3) / 2
                    xp < 200 -> xp * 2
                    xp < 400 -> xp * 3
                    else -> xp * 4
                },
                3000,
            )
        reduceChance *= 2 + Misc.random(8)

        if (Misc.chance(6500 - minOf(reduceChance, 6000)) == 1) {
            if (client.isInCombat()) {
                client.genieCombatFlag = true
                client.sendMessage("You got a genie random! Stop attack for a bit to get it.")
            } else {
                show(client)
            }
            client.chestEvent = 0
        }

        if (client.chestEvent >= 50) {
            val chance = Misc.chance(100)
            val trigger = (client.chestEvent - 50) * 2
            if (trigger >= chance) {
                client.chestEventOccur = true
                client.chestEvent = 0
                client.sendMessage("The server randomly detect you standing still for to long! Please move!")
            }
        }
    }
}
