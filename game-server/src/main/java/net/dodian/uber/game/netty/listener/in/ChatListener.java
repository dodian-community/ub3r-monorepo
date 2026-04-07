package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.systems.net.PacketChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Native Netty listener for public chat messages (opcode 4).
 * Decodes color, effects and chat string, then delegates to PacketChatService.
 */
@PacketHandler(opcode = 4)
public class ChatListener implements PacketListener {

    private static final Logger logger = LoggerFactory.getLogger(ChatListener.class);
    private static final int MIN_PAYLOAD_BYTES = 3;

    static {
        PacketListenerManager.register(4, new ChatListener());
    }

    @Override
    public void handle(Client client, GamePacket packet) throws Exception {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < MIN_PAYLOAD_BYTES) {
            return;
        }

        int color = buf.readUnsignedByte();
        int effects = buf.readUnsignedByte();
        String chat = ByteBufReader.readTerminatedString(buf, 256);
        byte[] chatBytes = chat.getBytes();

        PacketChatService.handlePublicChat(client, color, effects, chat, chatBytes);

        if (logger.isDebugEnabled()) {
            logger.debug("Chat from {}: {}", client.getPlayerName(), chat);
        }
    }
}
