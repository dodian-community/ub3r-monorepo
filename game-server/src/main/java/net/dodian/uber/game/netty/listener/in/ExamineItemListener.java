package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PacketHandler(opcode = 2)
public final class ExamineItemListener implements PacketListener {

    private static final Logger logger = LoggerFactory.getLogger(ExamineItemListener.class);

    static {
        PacketListenerManager.register(2, new ExamineItemListener());
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        try {
            ByteBuf buf = packet.getPayload();

            if (packet.getSize() < 2 || buf.readableBytes() < 2) {
                logger.warn("ExamineItem packet too small (size={}) from {}", packet.getSize(), client.getPlayerName());
                return;
            }

            int itemId = buf.readUnsignedShort();

            // Mystic client only sends the item id; use amount=1 so we fall back to
            // the normal examine text path on the server.
            int amount = 1;

            client.examineItem(client, itemId, amount);
        } catch (Exception e) {
            logger.error("Error handling ExamineItem packet for {}", client.getPlayerName(), e);
        }
    }
}
