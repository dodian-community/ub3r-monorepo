package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;

/**
 * Sends interface opcode (97) to open a specific interface on the client.
 * This replaces the legacy client.showInterface(int) implementation
 * and uses the complete Netty pipeline (ByteMessage -> ByteMessageEncoder -> GamePacketEncoder).
 */
public class ShowInterface implements OutgoingPacket {

    private final int interfaceId;

    public ShowInterface(int interfaceId) {
        this.interfaceId = interfaceId;
    }

    @Override
    public void send(Client client) {
        //System.out.println("Show interface: " + interfaceId);
        ByteMessage message = ByteMessage.message(97);
        message.putShort(interfaceId);
        client.send(message);
    }
}
