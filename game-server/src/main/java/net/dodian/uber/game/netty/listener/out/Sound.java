package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;

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
        this(soundId, 4, delay);
    }

    public Sound(int soundId) {
        this(soundId, 4, 0);
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(174);
        message.putShort(soundId);
        message.put(volume);
        message.putShort(delay);
        client.send(message);
    }

}
