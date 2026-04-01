package net.dodian.uber.game.systems.skills

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage

fun Client.sendFilterMessage(message: String) {
    send(SendMessage(message))
}
