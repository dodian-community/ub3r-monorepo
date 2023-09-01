package net.dodian.uber.game.model.client

import io.netty.channel.Channel
import net.dodian.uber.game.model.entity.player.Player

data class Client(
    val player: Player,
    val channel: Channel
)
