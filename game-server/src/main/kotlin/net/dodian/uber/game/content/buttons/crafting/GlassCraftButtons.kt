package net.dodian.uber.game.content.buttons.crafting

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage

object GlassCraftButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(
        44210, 44209, 44208, 44207,
        48108, 48107, 48106, 48105,
        48112, 48111, 48110, 48109,
        48116, 48115, 48114, 48113,
    )

    override fun onClick(client: Client, buttonId: Int): Boolean {
        when (buttonId) {
            44210, 44209, 44208, 44207 -> {
                client.send(RemoveInterfaces())
                val craftVialAmount = intArrayOf(27, 10, 5, 1)
                client.setSkill(Skill.CRAFTING.getId(), 229, 1, 1775, -1, 80, 884, 3)
                client.skillActionCount = craftVialAmount[buttonId - 44207]
                client.skillActionTimer = client.playerSkillAction[7]
                return true
            }

            48108, 48107, 48106, 48105 -> {
                client.send(RemoveInterfaces())
                if (client.getLevel(Skill.CRAFTING) < 18) {
                    client.send(SendMessage("You need level 18 crafting to craft a empty cup."))
                    return true
                }
                val craftCupAmount = intArrayOf(27, 10, 5, 1)
                client.setSkill(Skill.CRAFTING.getId(), 1980, 1, 1775, -1, 120, 884, 3)
                client.skillActionCount = craftCupAmount[buttonId - 48105]
                client.skillActionTimer = client.playerSkillAction[7]
                return true
            }

            48112, 48111, 48110, 48109 -> {
                client.send(RemoveInterfaces())
                if (client.getLevel(Skill.CRAFTING) < 32) {
                    client.send(SendMessage("You need level 32 crafting to craft a fishbowl."))
                    return true
                }
                val craftFishAmount = intArrayOf(27, 10, 5, 1)
                client.setSkill(Skill.CRAFTING.getId(), 6667, 1, 1775, -1, 160, 884, 3)
                client.skillActionCount = craftFishAmount[buttonId - 48109]
                client.skillActionTimer = client.playerSkillAction[7]
                return true
            }

            48116, 48115, 48114, 48113 -> {
                client.send(RemoveInterfaces())
                if (client.getLevel(Skill.CRAFTING) < 48) {
                    client.send(SendMessage("You need level 48 crafting to craft a unpowered orb."))
                    return true
                }
                val craftOrbAmount = intArrayOf(27, 10, 5, 1)
                client.setSkill(Skill.CRAFTING.getId(), 567, 1, 1775, -1, 240, 884, 3)
                client.skillActionCount = craftOrbAmount[buttonId - 48113]
                client.skillActionTimer = client.playerSkillAction[7]
                return true
            }
        }
        return false
    }
}

