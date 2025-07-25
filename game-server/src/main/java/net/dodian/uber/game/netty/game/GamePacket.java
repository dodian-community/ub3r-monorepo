package net.dodian.uber.game.netty.game;

import io.netty.buffer.ByteBuf;

/**
 * Lightweight container for an incoming RuneScape game packet.
 */
public class GamePacket {

    private final int opcode;
    private final int size;
    private final ByteBuf payload;

    public GamePacket(int opcode, int size, ByteBuf payload) {
        this.opcode = opcode;
        this.size = size;
        this.payload = payload;
    }

    public int getOpcode() {
        return opcode;
    }

    public int getSize() {
        return size;
    }

    public ByteBuf getPayload() {
        return payload;
    }
}
