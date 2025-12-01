package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PacketHandler(opcode = 6)
public final class ExamineNpcListener implements PacketListener {

    private static final Logger logger = LoggerFactory.getLogger(ExamineNpcListener.class);

    static {
        PacketListenerManager.register(6, new ExamineNpcListener());
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        try {
            ByteBuf buf = packet.getPayload();

            if (packet.getSize() < 2 || buf.readableBytes() < 2) {
                logger.warn("ExamineNpc packet too small (size={}) from {}", packet.getSize(), client.getPlayerName());
                return;
            }

            int npcId = buf.readUnsignedShort();

            client.examineNpc(client, npcId);
        } catch (Exception e) {
            logger.error("Error handling ExamineNpc packet for {}", client.getPlayerName(), e);
        }
    }
}
