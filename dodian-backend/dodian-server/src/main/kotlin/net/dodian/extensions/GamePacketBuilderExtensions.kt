package net.dodian.extensions

import net.dodian.uber.net.codec.game.GamePacket
import net.dodian.uber.net.codec.game.GamePacketBuilder

val GamePacketBuilder.built: GamePacket get() = this.toGamePacket()