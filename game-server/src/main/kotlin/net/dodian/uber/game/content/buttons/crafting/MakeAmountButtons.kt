package net.dodian.uber.game.content.buttons.crafting

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces

object MakeAmountButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(10239, 10238, 6212, 6211)

    override fun onClick(client: Client, buttonId: Int): Boolean {
        if (client.playerSkillAction.isEmpty()) {
            return true
        }
        client.send(RemoveInterfaces())
        client.skillActionCount = when (buttonId) {
            10239 -> 1
            10238 -> 5
            6212 -> 10
            6211 -> 28
            else -> return false
        }
        client.skillActionTimer = client.playerSkillAction[7]
        return true
    }
}

