package net.dodian.uber.game.netty.game;

import io.netty.buffer.ByteBuf;

/**
 * Lightweight container for an incoming RuneScape game packet.
 */
public record GamePacket(int opcode, int size, ByteBuf payload) {

}
