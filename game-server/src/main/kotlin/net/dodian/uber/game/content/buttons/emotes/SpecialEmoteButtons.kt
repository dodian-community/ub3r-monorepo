package net.dodian.uber.game.content.buttons.emotes

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.player.content.Skillcape
import net.dodian.uber.game.netty.listener.out.SendMessage

object SpecialEmoteButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(
        88060, 88061, 88062, 88063,
        59062, 72254, 72033, 72032,
        74108,
    )

    override fun onClick(client: Client, buttonId: Int): Boolean {
        when (buttonId) {
            88060 -> {
                client.requestAnim(4276, 0)
                client.gfx0(712)
            }
            88061 -> {
                client.requestAnim(4278, 0)
                client.gfx0(713)
            }
            88062 -> client.requestAnim(4280, 0)
            88063 -> client.requestAnim(4275, 0)
            59062 -> client.requestAnim(2836, 0)
            72254 -> client.requestAnim(6111, 0)
            72033 -> client.requestAnim(3543, 0)
            72032 -> client.requestAnim(3544, 0)
            74108 -> {
                var skillcape = Skillcape.getSkillCape(client.equipment[Equipment.Slot.CAPE.id])
                if (skillcape != null) {
                    client.requestAnim(skillcape.emote, 0)
                    client.gfx0(skillcape.gfx)
                } else if (client.equipment[Equipment.Slot.CAPE.id] == 9813) {
                    client.requestAnim(4945, 0)
                    client.gfx0(816)
                } else if (client.GetItemName(client.equipment[Equipment.Slot.CAPE.id]).lowercase().contains("max cape")) {
                    skillcape = Skillcape.getRandomCape()
                    client.requestAnim(skillcape.emote, 0)
                    client.gfx0(skillcape.gfx)
                } else {
                    client.send(SendMessage("You need to be wearing a skillcape to do that!"))
                }
            }
            else -> return false
        }
        return true
    }
}

