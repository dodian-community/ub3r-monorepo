package net.dodian.uber.game.skill.runtime.action

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendString
import net.dodian.uber.game.engine.util.Misc
import net.dodian.utilities.Utils

object SkillingRandomEventService {
    @JvmStatic
    fun show(client: Client) {
        client.resetAction(true)
        val state = client.skillingEventState
        if (state.isRandomEventOpen && client.randomed2) {
            return
        }
        val randomSkill = Utils.random(20)
        client.skillingEventState = state.withRandomSkillId(randomSkill).withRandomEventOpen(true)
        client.sendString("Click the @or1@${Skill.getSkill(randomSkill)?.name ?: "skill"} @yel@button", 2810)
        client.sendString("", 2811)
        client.sendString("", 2831)
        client.clearTabs()
        client.openInterface(2808)
    }

    @JvmStatic
    fun trigger(client: Client, awardedExperience: Int) {
        var state = client.skillingEventState
        if (client.genieCombatFlag || state.isRandomEventOpen) {
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
            if (client.isInCombat) {
                client.genieCombatFlag = true
                client.sendMessage("You got a genie random! Stop attack for a bit to get it.")
            } else {
                show(client)
            }
            state = client.skillingEventState.withChestEventCount(0)
            client.skillingEventState = state
        }

        if (state.chestEventCount >= 50) {
            val chance = Misc.chance(100)
            val trigger = (state.chestEventCount - 50) * 2
            if (trigger >= chance) {
                state =
                    state
                        .withChestEventPendingMove(true)
                        .withChestEventCount(0)
                client.skillingEventState = state
                client.sendMessage("The server randomly detect you standing still for to long! Please move!")
            }
        }
    }
}
