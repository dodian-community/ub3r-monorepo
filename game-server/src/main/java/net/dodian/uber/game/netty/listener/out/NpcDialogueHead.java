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
        // Try without ValueType.ADD - method436() might expect normal shorts
        message.putShort(mainFrame, ByteOrder.BIG, ValueType.NORMAL);
        message.putShort(subFrame, ByteOrder.BIG, ValueType.NORMAL);
        client.send(message);
    }

}
