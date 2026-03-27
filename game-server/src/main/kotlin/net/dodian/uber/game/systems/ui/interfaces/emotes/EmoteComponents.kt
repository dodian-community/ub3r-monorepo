package net.dodian.uber.game.systems.ui.interfaces.emotes

import net.dodian.uber.game.model.entity.player.Emotes

object EmoteComponents {
    val standardEmoteButtons: IntArray = Emotes.values().map { it.buttonId }.toIntArray()
    val goblinBowButtons = intArrayOf(88060)
    val goblinSaluteButtons = intArrayOf(88061)
    val glassBoxButtons = intArrayOf(88062)
    val climbRopeButtons = intArrayOf(88063)
    val leanButtons = intArrayOf(59062)
    val glassWallButtons = intArrayOf(72254)
    val ideaButtons = intArrayOf(72033)
    val stompButtons = intArrayOf(72032)
    val skillcapeButtons = intArrayOf(74108)
}
