package net.dodian.uber.game.model.player.packets.incoming;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.ChatLine;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.networking.game.ByteBufPacket;
import net.dodian.uber.game.security.ChatLog;
import net.dodian.utilities.Utils;

/**
 * ByteBuf-based replacement for legacy {@code Chat} packet (opcode 4).
 */
public class Chat implements ByteBufPacket {

    @Override
    public void process(Client client, int opcode, int size, ByteBuf payload) {
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

        // Effect & colour bytes use the RS317 'S' variant (128 - value)
        int effects = 128 - payload.readUnsignedByte();
        int colour  = 128 - payload.readUnsignedByte();
        int textSize = size - 2;

        byte[] chatBytes = new byte[textSize];
        // readBytes_reverseA: iterate from end to start, subtract 128
        for (int i = textSize - 1; i >= 0; i--) {
            chatBytes[i] = (byte) (payload.readByte() - 128);
        }

        String chat = Utils.textUnpack(chatBytes, textSize);

        client.setChatTextEffects(effects);
        client.setChatTextColor(colour);
        client.setChatTextSize((byte) textSize);
        System.arraycopy(chatBytes, 0, client.getChatText(), 0, textSize);

        client.getUpdateFlags().setRequired(UpdateFlag.CHAT, true);
        ChatLog.recordPublicChat(client, chat);
        Server.chat.add(new ChatLine(client.getPlayerName(), client.dbId, 2, chat,
                client.getPosition().getX(), client.getPosition().getY()));
    }
}
