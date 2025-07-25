package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ValueType;

/**
 * Sends opcode 249 (player membership status and index) on login.
 */
public class PlayerDetails implements OutgoingPacket {

    private final int memberFlag;
    private final int slot;

    public PlayerDetails(int memberFlag, int slot) {
        this.memberFlag = memberFlag;
        this.slot = slot;
    }

    @Override
    public void send(Client client) {
        ByteMessage msg = ByteMessage.message(249);
        msg.put(memberFlag, ValueType.ADD);      // writeByteA
        msg.putShort(slot, ValueType.ADD);       // writeWordBigEndianA
        client.send(msg);
    }
}
