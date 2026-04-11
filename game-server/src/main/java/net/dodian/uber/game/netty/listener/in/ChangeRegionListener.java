package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.engine.systems.net.PacketConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty replacement for legacy ChangeRegion packet (opcodes 121, 210).
 * Packet has no payload; behaviour is identical to legacy handler.
 */
@PacketHandler(opcode = 121) // primary opcode (loads new area)
public class ChangeRegionListener implements PacketListener {

    private static final Logger logger = LoggerFactory.getLogger(ChangeRegionListener.class);

    static {
        ChangeRegionListener l = new ChangeRegionListener();
        PacketListenerManager.register(121, l);
        PacketListenerManager.register(210, l); // second opcode used for same logic
    }

    @Override
    public void handle(Client client, GamePacket packet) throws Exception {
        // No payload to read; ensure we consume any bytes if size>0 just in case
        ByteBuf buf = packet.payload();
        if (buf.isReadable()) {
            buf.skipBytes(buf.readableBytes());
        }

        PacketConnectionService.handleRegionChange(client, packet.opcode() == 121);
    }
}

