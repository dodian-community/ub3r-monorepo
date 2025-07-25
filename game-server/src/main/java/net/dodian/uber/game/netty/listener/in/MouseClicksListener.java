package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty implementation of legacy MouseClicks packet (opcode 241).
 * Just consumes an int click-id and optionally logs it when SERVER_ENV=dev.
 */
public class MouseClicksListener implements PacketListener {

    static { PacketListenerManager.register(241, new MouseClicksListener()); }

    private static final Logger logger = LoggerFactory.getLogger(MouseClicksListener.class);

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();
        int clickId = buf.readInt(); // same as readDWord

        String env = System.getenv().getOrDefault("SERVER_ENV", "");
        if ("dev".equalsIgnoreCase(env)) {
            logger.debug("MouseClicks id {} from {}", clickId, client.getPlayerName());
        }
    }
}
