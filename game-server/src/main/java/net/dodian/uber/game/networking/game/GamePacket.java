package net.dodian.uber.game.networking.game;

import io.netty.buffer.ByteBuf;

/**
 * Lightweight container for an incoming RuneScape game packet.
 * This class purposely carries only opcode, size and payload
 * so that downstream handlers can remain agnostic of Netty internals
 * while still benefiting from zero-copy {@link ByteBuf} slicing.
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
