package net.dodian.uber.game.network.packets.outgoing;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.network.packets.OutgoingPacket;

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
        client.getOutputStream().createFrame(174);
        client.getOutputStream().writeWord(songId + 10000);
        client.getOutputStream().writeByte(enabled);
        client.getOutputStream().writeWord(unlocked);
    }

}
