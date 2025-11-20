package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;

/**
 * Sends the player's run energy to the client.
 * Client opcode: 110 (PacketConstants.SEND_RUN_ENERGY)
 */
public class SendRunEnergy implements OutgoingPacket {

    private final int energy; // 0-100

    public SendRunEnergy(int energy) {
        this.energy = energy;
    }

    @Override
    public void send(Client client) {
        ByteMessage msg = ByteMessage.message(110);
        msg.put(energy & 0xFF);
        client.send(msg);
    }
}
