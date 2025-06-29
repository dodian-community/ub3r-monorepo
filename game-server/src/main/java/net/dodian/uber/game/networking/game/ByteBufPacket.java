package net.dodian.uber.game.networking.game;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;

/**
 * Contract for processing an inbound GamePacket whose payload is still a Netty ByteBuf.
 * Implementations should NOT mutate the ByteBuf reader index outside of their own reads.
 */
public interface ByteBufPacket {
    /**
     * Handle the inbound packet.
     *
     * @param client  The in-game player client.
     * @param opcode  Packet opcode.
     * @param size    Declared payload size.
     * @param payload Zero-copy ByteBuf containing the payload (reader index at 0).
     */
    void process(Client client, int opcode, int size, ByteBuf payload);
}
