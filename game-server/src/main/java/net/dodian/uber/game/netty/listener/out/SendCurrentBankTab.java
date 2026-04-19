package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.listener.OutgoingPacket;

/**
 * Sends the currently-selected bank tab to the Mystic client (opcode 55).
 */
public class SendCurrentBankTab implements OutgoingPacket {

    private final int tab;

    public SendCurrentBankTab(int tab) {
        this.tab = tab;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(55);
        message.put(tab);
        client.send(message);
    }
}
