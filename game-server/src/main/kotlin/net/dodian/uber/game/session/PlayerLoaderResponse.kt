package net.dodian.uber.game.session

import net.dodian.uber.game.model.entity.player.Player

data class PlayerLoaderResponse(
    val status: Int,
    val player: Player? = null
)