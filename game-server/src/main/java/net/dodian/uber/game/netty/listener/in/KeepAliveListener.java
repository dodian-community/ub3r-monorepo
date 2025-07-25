package net.dodian.uber.game.netty.listener.in;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles opcode 0 keep-alive packets (no data).
 */
@PacketHandler(opcode = 0)
public final class KeepAliveListener implements PacketListener {

    private static final Logger logger = LoggerFactory.getLogger(KeepAliveListener.class);

    static {
        // Manual registration until reflection-based auto register is built
        PacketListenerManager.register(0, new KeepAliveListener());
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        // Reset timeout counter so server knows the client is alive
        client.timeOutCounter = 0;
         }
    }

