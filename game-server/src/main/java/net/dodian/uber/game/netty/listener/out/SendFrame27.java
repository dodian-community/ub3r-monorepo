package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;

/**
 * Sends opcode 27 (SEND_ENTER_AMOUNT) to prompt the client for a numeric input.
 * Mystic client expects: String title (the prompt text to display)
 * 
 * Packet structure:
 * - Opcode: 27 (variable byte size)
 * - String: prompt title (e.g., "Enter amount:")
 */
public class SendFrame27 implements OutgoingPacket {

    private final String title;

    public SendFrame27() {
        this.title = "Enter amount:";
    }

    public SendFrame27(String title) {
        this.title = title != null ? title : "Enter amount:";
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(27, MessageType.VAR);
        message.putString(title);
        client.send(message);
    }
}