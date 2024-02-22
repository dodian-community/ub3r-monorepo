package net.dodian.uber.game.model.player.packets.outgoing;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.OutgoingPacket;

/**
 * @author Dashboard
 */
public class Sound implements OutgoingPacket {

    public final int soundId, delay, volume;

    public Sound(int soundId, int volume, int delay) {
        this.soundId = soundId;
        this.volume = volume;
        this.delay = delay;
    }

    public Sound(int soundId, int delay) {
        this(soundId, 10, delay);
    }

    public Sound(int soundId) {
        this(soundId, 10, 0);
    }

    @Override
    public void send(Client client) {
        client.getOutputStream().createFrame(174);
        client.getOutputStream().writeWord(soundId);
        client.getOutputStream().writeByte(volume);
        client.getOutputStream().writeWord(delay);
    }

}
