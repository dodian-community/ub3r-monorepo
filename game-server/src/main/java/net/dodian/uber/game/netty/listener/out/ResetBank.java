package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.netty.codec.ValueType;

/**
 * Sent to update the client's bank interface with the current bank contents.
 */
public class ResetBank implements OutgoingPacket {

    @Override
    public void send(Client client) {
        client.sendBankContainers();
    }
}
