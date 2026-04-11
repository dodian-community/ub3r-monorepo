package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.engine.systems.net.PacketAppearanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Native Netty listener for the "change appearance" packet.
 * Migrates the legacy {@code ChangeAppearance} Stream-based handler to Netty.
 */
@PacketHandler(opcode = 11)
public class ChangeAppearanceListener implements PacketListener {

    private static final Logger logger = LoggerFactory.getLogger(ChangeAppearanceListener.class);

    /*
     * Register explicitly so the LegacyBridgeListener does not claim opcode 11.
     */
    static {
        PacketListenerManager.register(11, new ChangeAppearanceListener());
    }

    // Packet is a fixed 13 bytes (each a signed byte in legacy stream)
    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < 13) {
            logger.warn("ChangeAppearance packet too short from {} ({} bytes)", client.getPlayerName(), buf.readableBytes());
            return;
        }

        int[] looks = new int[13];
        for (int i = 0; i < 13; i++) {
            looks[i] = buf.readUnsignedByte();
        }

        PacketAppearanceService.handleAppearanceChange(client, looks);
    }
}

