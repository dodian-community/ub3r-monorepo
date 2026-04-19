package net.dodian.uber.game.content.ui

import net.dodian.uber.game.model.entity.player.Emotes
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.content.skills.Skillcape
import net.dodian.uber.game.content.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.content.ui.buttons.buttonBinding

object EmoteInterface : InterfaceButtonContent {
    private val standardEmoteButtons: IntArray = Emotes.values().map { it.buttonId }.toIntArray()
    private val goblinBowButtons = intArrayOf(88060)
    private val goblinSaluteButtons = intArrayOf(88061)
    private val glassBoxButtons = intArrayOf(88062)
    private val climbRopeButtons = intArrayOf(88063)
    private val leanButtons = intArrayOf(59062)
    private val glassWallButtons = intArrayOf(72254)
    private val ideaButtons = intArrayOf(72033)
    private val stompButtons = intArrayOf(72032)
    private val skillcapeButtons = intArrayOf(74108)

    override val bindings =
        listOf(
            buttonBinding(-1, 0, "emotes.standard", standardEmoteButtons) { client, request ->
                Emotes.doEmote(request.rawButtonId, client)
                true
            },
            buttonBinding(-1, 1, "emotes.special.goblin_bow", goblinBowButtons) { client, _ ->
                client.performAnimation(4276, 0)
                client.gfx0(712)
                true
            },
            buttonBinding(-1, 2, "emotes.special.goblin_salute", goblinSaluteButtons) { client, _ ->
                client.performAnimation(4278, 0)
                client.gfx0(713)
                true
            },
            buttonBinding(-1, 3, "emotes.special.glass_box", glassBoxButtons) { client, _ ->
                client.performAnimation(4280, 0)
                true
            },
            buttonBinding(-1, 4, "emotes.special.climb_rope", climbRopeButtons) { client, _ ->
                client.performAnimation(4275, 0)
                true
            },
            buttonBinding(-1, 5, "emotes.special.lean", leanButtons) { client, _ ->
                client.performAnimation(2836, 0)
                true
            },
            buttonBinding(-1, 6, "emotes.special.glass_wall", glassWallButtons) { client, _ ->
                client.performAnimation(6111, 0)
                true
            },
            buttonBinding(-1, 7, "emotes.special.idea", ideaButtons) { client, _ ->
                client.performAnimation(3543, 0)
                true
            },
            buttonBinding(-1, 8, "emotes.special.stomp", stompButtons) { client, _ ->
                client.performAnimation(3544, 0)
                true
            },
            buttonBinding(-1, 9, "emotes.special.skillcape", skillcapeButtons) { client, _ ->
                var skillcape = Skillcape.getSkillCape(client.equipment[Equipment.Slot.CAPE.id])
                if (skillcape != null) {
                    client.performAnimation(skillcape.emote, 0)
                    client.gfx0(skillcape.gfx)
                } else if (client.equipment[Equipment.Slot.CAPE.id] == 9813) {
                    client.performAnimation(4945, 0)
                    client.gfx0(816)
                } else if (client.getItemName(client.equipment[Equipment.Slot.CAPE.id]).lowercase().contains("max cape")) {
                    skillcape = Skillcape.getRandomCape()
                    client.performAnimation(skillcape.emote, 0)
                    client.gfx0(skillcape.gfx)
                } else {
                    client.sendMessage("You need to be wearing a skillcape to do that!")
                }
                true
            },
        )
}
