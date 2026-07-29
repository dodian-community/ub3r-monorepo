@file:Suppress("unused")

package net.dodian.uber.game.ui

import net.dodian.uber.game.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.ui.buttons.buttonBinding

import net.dodian.uber.skills.api.Skillcape
import net.dodian.uber.game.model.entity.player.Emotes
import net.dodian.uber.game.model.item.Equipment

object EmoteInterface : InterfaceButtonContent {
    private val standardEmoteButtons: IntArray = Emotes.values().map { it.buttonId }.toIntArray()
    // Old IDs (all wrong — from stale pre-client-update values):
    //   goblinBow=88060, goblinSalute=88061, glassBox=88062, climbRope=88063,
    //   lean=59062, glassWall=72254, idea=72033, stomp=72032, skillcape=74108
    // New IDs from dump ROOT 147:
    private val goblinBowButtons = intArrayOf(13383)
    private val goblinSaluteButtons = intArrayOf(13384)
    private val glassBoxButtons = intArrayOf(667)
    private val climbRopeButtons = intArrayOf(6503)
    private val leanButtons = intArrayOf(6506)
    private val glassWallButtons = intArrayOf(666)
    private val ideaButtons = intArrayOf(18700)
    private val stompButtons = intArrayOf(18701)
    private val skillcapeButtons = intArrayOf(19052)

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
            // Unlockable emotes present in ROOT 147 that had no handler (buttons from new client).
            // Animation IDs from Tarnish Emote.java.
            buttonBinding(-1, 10, "emotes.special.scared", intArrayOf(15166)) { client, _ ->
                client.performAnimation(2836, 0)
                true
            },
            buttonBinding(-1, 11, "emotes.special.zombie_walk", intArrayOf(18464)) { client, _ ->
                client.performAnimation(3544, 0)
                true
            },
            buttonBinding(-1, 12, "emotes.special.zombie_dance", intArrayOf(18465)) { client, _ ->
                client.performAnimation(3543, 0)
                true
            },
            buttonBinding(-1, 13, "emotes.special.bunny_hop", intArrayOf(18686)) { client, _ ->
                client.performAnimation(6111, 0)
                true
            },
            buttonBinding(-1, 14, "emotes.special.sit_up", intArrayOf(22588)) { client, _ ->
                client.performAnimation(2763, 0)
                true
            },
            buttonBinding(-1, 15, "emotes.special.push_up", intArrayOf(22589)) { client, _ ->
                client.performAnimation(2756, 0)
                true
            },
            buttonBinding(-1, 16, "emotes.special.star_jump", intArrayOf(22590)) { client, _ ->
                client.performAnimation(2761, 0)
                true
            },
            buttonBinding(-1, 17, "emotes.special.jog", intArrayOf(22591)) { client, _ ->
                client.performAnimation(2764, 0)
                true
            },
            buttonBinding(-1, 18, "emotes.special.zombie_hand", intArrayOf(22593)) { client, _ ->
                client.performAnimation(4513, 0)
                client.gfx0(320)
                true
            },
        )
}
