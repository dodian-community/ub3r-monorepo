package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.netty.listener.OutgoingPacket;

/**
 * Sends opcode 187 (SEND_ENTER_NAME) for text-input prompts.
 */
public class SendEnterName implements OutgoingPacket {

    private final String title;

    public SendEnterName(String title) {
        this.title = title != null ? title : "Enter name:";
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(187, MessageType.VAR);
        message.putString(title);
        client.send(message);
    }
}
