package net.dodian.uber.game.content.buttons.crafting

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.runtime.action.ProductionActionService
import net.dodian.uber.game.runtime.action.ProductionMode
import net.dodian.uber.game.runtime.action.ProductionRequest

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
                ProductionActionService.start(
                    client,
                    ProductionRequest(
                        skillId = Skill.CRAFTING.id,
                        productId = 229,
                        amountPerCycle = 1,
                        primaryItemId = 1775,
                        secondaryItemId = -1,
                        experiencePerUnit = 80,
                        animationId = 884,
                        tickDelay = 3,
                    ),
                    craftVialAmount[buttonId - 44207],
                )
                return true
            }

            48108, 48107, 48106, 48105 -> {
                client.send(RemoveInterfaces())
                if (client.getLevel(Skill.CRAFTING) < 18) {
                    client.send(SendMessage("You need level 18 crafting to craft a empty cup."))
                    return true
                }
                val craftCupAmount = intArrayOf(27, 10, 5, 1)
                ProductionActionService.start(
                    client,
                    ProductionRequest(
                        skillId = Skill.CRAFTING.id,
                        productId = 1980,
                        amountPerCycle = 1,
                        primaryItemId = 1775,
                        secondaryItemId = -1,
                        experiencePerUnit = 120,
                        animationId = 884,
                        tickDelay = 3,
                    ),
                    craftCupAmount[buttonId - 48105],
                )
                return true
            }

            48112, 48111, 48110, 48109 -> {
                client.send(RemoveInterfaces())
                if (client.getLevel(Skill.CRAFTING) < 32) {
                    client.send(SendMessage("You need level 32 crafting to craft a fishbowl."))
                    return true
                }
                val craftFishAmount = intArrayOf(27, 10, 5, 1)
                ProductionActionService.start(
                    client,
                    ProductionRequest(
                        skillId = Skill.CRAFTING.id,
                        productId = 6667,
                        amountPerCycle = 1,
                        primaryItemId = 1775,
                        secondaryItemId = -1,
                        experiencePerUnit = 160,
                        animationId = 884,
                        tickDelay = 3,
                    ),
                    craftFishAmount[buttonId - 48109],
                )
                return true
            }

            48116, 48115, 48114, 48113 -> {
                client.send(RemoveInterfaces())
                if (client.getLevel(Skill.CRAFTING) < 48) {
                    client.send(SendMessage("You need level 48 crafting to craft a unpowered orb."))
                    return true
                }
                val craftOrbAmount = intArrayOf(27, 10, 5, 1)
                ProductionActionService.start(
                    client,
                    ProductionRequest(
                        skillId = Skill.CRAFTING.id,
                        productId = 567,
                        amountPerCycle = 1,
                        primaryItemId = 1775,
                        secondaryItemId = -1,
                        experiencePerUnit = 240,
                        animationId = 884,
                        tickDelay = 3,
                    ),
                    craftOrbAmount[buttonId - 48113],
                )
                return true
            }
        }
        return false
    }
}
