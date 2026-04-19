package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ValueType;

public class SetTabInterface implements OutgoingPacket {

    private final int mainFrame;
    private final int subFrame;

    public SetTabInterface(int mainFrame, int subFrame) {
        this.mainFrame = mainFrame;
        this.subFrame = subFrame;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(248);
        message.putShort(mainFrame, ValueType.ADD);
        message.putShort(subFrame);
        client.send(message);
    }
}
