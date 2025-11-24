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

    @Override
    public void handle(Client client, GamePacket packet) throws Exception {
        ByteBuf buf = packet.getPayload();

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

        // Mystic client sends color then effect as raw bytes, followed by a null-terminated string.
        int color = buf.readUnsignedByte();
        int effects = buf.readUnsignedByte();
        client.setChatTextEffects(effects);
        client.setChatTextColor(color);

        String chat = readTerminatedString(buf);
        byte[] chatBytes = chat.getBytes();
        int copyLen = Math.min(chatBytes.length, client.getChatText().length);
        client.setChatTextSize(copyLen);
        if (copyLen > 0) {
            System.arraycopy(chatBytes, 0, client.getChatText(), 0, copyLen);
        }
        client.setChatTextMessage(chat);
        // Ensure any cached update block is rebuilt so viewers receive this chat.
        client.invalidateCachedUpdateBlock();
        client.getUpdateFlags().setRequired(UpdateFlag.CHAT, true);
        ChatLog.recordPublicChat(client, chat);

        // Add to global chat history (same as legacy)
        Server.chat.add(new ChatLine(client.getPlayerName(), client.dbId, 2, chat,
                client.getPosition().getX(), client.getPosition().getY()));

        if (logger.isDebugEnabled()) {
            logger.debug("Chat from {}: {}", client.getPlayerName(), chat);
        }
    }

    private String readTerminatedString(ByteBuf buf) {
        StringBuilder sb = new StringBuilder();
        while (buf.isReadable()) {
            byte b = buf.readByte();
            if (b == 10 || b == 0) {
                break;
            }
            sb.append((char) b);
        }
        return sb.toString();
    }
}
