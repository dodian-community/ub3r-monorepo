package net.dodian.uber.game.model.player.packets.incoming;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.networking.game.ByteBufPacket;

/**
 * ByteBuf wrapper for the legacy {@link ClickingButtons} handler.
 * <p>
 * The original implementation relies on {@link Client#getInputStream()} so we
 * simply copy the payload into that buffer then invoke the original handler.
 * This keeps the large button-handling logic untouched while allowing opcode
 * 185 to flow through the new Netty pipeline without hitting the generic
 * fallback bridge.
 */
public class ClickingButtonsBuf implements ByteBufPacket {

    // Re-use single instance of the legacy handler
    private static final ClickingButtons DELEGATE = new ClickingButtons();

    @Override
    public void process(Client client, int opcode, int size, ByteBuf payload) {
        // Copy payload to the client's Stream buffer exactly like it arrived
        byte[] data = new byte[size];
        payload.readBytes(data);
        client.getInputStream().currentOffset = 0;
        client.getInputStream().buffer = data;

        // Delegate to legacy logic (expects Stream-backed data)
        DELEGATE.ProcessPacket(client, opcode, size);
    }
}
