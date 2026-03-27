package net.dodian.uber.game.systems.ui.interfaces.emotes

import net.dodian.uber.game.model.entity.player.Emotes
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.player.content.Skillcape
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.systems.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.systems.ui.buttons.buttonBinding

object EmoteInterfaceButtons : InterfaceButtonContent {
    override val bindings =
        listOf(
            buttonBinding(-1, 0, "emotes.standard", EmoteComponents.standardEmoteButtons) { client, request ->
                Emotes.doEmote(request.rawButtonId, client)
                true
            },
            buttonBinding(-1, 1, "emotes.special.goblin_bow", EmoteComponents.goblinBowButtons) { client, _ ->
                client.requestAnim(4276, 0)
                client.gfx0(712)
                true
            },
            buttonBinding(-1, 2, "emotes.special.goblin_salute", EmoteComponents.goblinSaluteButtons) { client, _ ->
                client.requestAnim(4278, 0)
                client.gfx0(713)
                true
            },
            buttonBinding(-1, 3, "emotes.special.glass_box", EmoteComponents.glassBoxButtons) { client, _ ->
                client.requestAnim(4280, 0)
                true
            },
            buttonBinding(-1, 4, "emotes.special.climb_rope", EmoteComponents.climbRopeButtons) { client, _ ->
                client.requestAnim(4275, 0)
                true
            },
            buttonBinding(-1, 5, "emotes.special.lean", EmoteComponents.leanButtons) { client, _ ->
                client.requestAnim(2836, 0)
                true
            },
            buttonBinding(-1, 6, "emotes.special.glass_wall", EmoteComponents.glassWallButtons) { client, _ ->
                client.requestAnim(6111, 0)
                true
            },
            buttonBinding(-1, 7, "emotes.special.idea", EmoteComponents.ideaButtons) { client, _ ->
                client.requestAnim(3543, 0)
                true
            },
            buttonBinding(-1, 8, "emotes.special.stomp", EmoteComponents.stompButtons) { client, _ ->
                client.requestAnim(3544, 0)
                true
            },
            buttonBinding(-1, 9, "emotes.special.skillcape", EmoteComponents.skillcapeButtons) { client, _ ->
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
                true
            },
        )
}
