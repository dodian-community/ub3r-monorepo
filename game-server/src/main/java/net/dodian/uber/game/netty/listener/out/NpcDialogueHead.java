package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;

public class NpcDialogueHead implements OutgoingPacket {

    private int mainFrame, subFrame;

    public NpcDialogueHead(int mainFrame, int subFrame) {
        this.mainFrame = mainFrame;
        this.subFrame = subFrame;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(75);
        // Client reads: readLEUShortA() for npcId, readLEUShortA() for interfaceId
        // LEUShortA = Little-Endian Unsigned Short with ADD transform on low byte
        message.putShort(mainFrame, ByteOrder.LITTLE, ValueType.ADD);
        message.putShort(subFrame, ByteOrder.LITTLE, ValueType.ADD);
        client.send(message);
    }

}
