package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;

/**
 * @author Dashboard
 */
public class SongSetting implements OutgoingPacket {

    private int songId;
    private int enabled;
    private int unlocked;

    public SongSetting(int songId, boolean enabled, boolean unlocked) {
        this.songId = songId;
        this.enabled = enabled ? 1 : 0;
        this.unlocked = unlocked ? 1 : 0;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(174);
        message.putShort(songId + 10000);
        message.put(enabled);
        message.putShort(unlocked);
        client.send(message);
    }

}
