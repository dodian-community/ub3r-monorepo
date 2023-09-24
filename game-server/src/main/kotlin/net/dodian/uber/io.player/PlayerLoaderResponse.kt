package net.dodian.uber.io.player

import net.dodian.uber.game.modelkt.entity.player.Player

data class PlayerLoaderResponse(
    val status: Int,
    val player: Player? = null
)