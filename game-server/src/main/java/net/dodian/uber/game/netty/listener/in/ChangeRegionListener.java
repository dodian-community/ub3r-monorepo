package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
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
        ByteBuf buf = packet.getPayload();
        if (buf.isReadable()) {
            buf.skipBytes(buf.readableBytes());
        }

        if (!client.pLoaded) {
            client.pLoaded = true;
        }
        int wild = client.getWildLevel();
        if (wild > 0) {
            client.setWildLevel(wild);
        } else {
            client.updatePlayerDisplay();
        }
        if (!client.IsPMLoaded) {
            client.refreshFriends();
            client.IsPMLoaded = true;
        }
        if (packet.getOpcode() == 121) {
            // Loads new area => spawn custom objects
            client.customObjects();
        }
    }
}
