package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;

public class SetRegionSong implements OutgoingPacket {

    private int songId;

    public SetRegionSong(int songId) {
        this.songId = songId;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(74);
        message.putShort(songId, ByteOrder.BIG);
        client.send(message);
    }

}
