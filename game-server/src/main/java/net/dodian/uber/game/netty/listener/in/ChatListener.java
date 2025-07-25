package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.ChatLine;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.security.ChatLog;
import net.dodian.utilities.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Native Netty listener for public chat messages (opcode 4).
 * Mirrors the behaviour of legacy {@code Chat} handler while removing Stream dependency.
 */
@PacketHandler(opcode = 4)
public class ChatListener implements PacketListener {

    private static final Logger logger = LoggerFactory.getLogger(ChatListener.class);

    static {
        // Ensure we beat LegacyBridge registration
        PacketListenerManager.register(4, new ChatListener());
    }

    private static int readUnsignedByteS(ByteBuf buf) {
        return (128 - buf.readUnsignedByte()) & 0xFF;
    }

    @Override
    public void handle(Client client, GamePacket packet) throws Exception {
        ByteBuf buf = packet.getPayload();
        int size = packet.getSize();

        if (!client.validClient) {
            client.send(new SendMessage("Please use another client"));
            return;
        }
        if (client.isMuted()) {
            client.send(new SendMessage("You are currently muted!"));
            return;
        }
        if (!Server.chatOn && client.playerRights == 0) {
            client.send(new SendMessage("Public chat has been temporarily restricted"));
            return;
        }

        // Read effects and color using S transform
        int effects = readUnsignedByteS(buf);
        int color   = readUnsignedByteS(buf);
        client.setChatTextEffects(effects);
        client.setChatTextColor(color);

        int textLen = size - 2;
        client.setChatTextSize(textLen);  // Removed dangerous byte cast
        byte[] chatBytes = new byte[textLen];
        // readBytes_reverseA: iterate from end to start, subtract 128
        for (int i = textLen - 1; i >= 0; i--) {
            chatBytes[i] = (byte) (buf.readByte() - 128);
        }

        String chat = Utils.textUnpack(chatBytes, textLen);
        // copy to client's chatText buffer for update blocks
        System.arraycopy(chatBytes, 0, client.getChatText(), 0, textLen);
        client.getUpdateFlags().setRequired(UpdateFlag.CHAT, true);
        ChatLog.recordPublicChat(client, chat);

        // Add to global chat history (same as legacy)
        Server.chat.add(new ChatLine(client.getPlayerName(), client.dbId, 2, chat,
                client.getPosition().getX(), client.getPosition().getY()));

        if (logger.isDebugEnabled()) {
            logger.debug("Chat from {}: {}", client.getPlayerName(), chat);
        }
    }
}
