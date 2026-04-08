package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.engine.event.GameEventBus;
import net.dodian.uber.game.events.widget.PrivateMessageEvent;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.utilities.Utils;

/**
 * Netty implementation of private-message send (opcode 126).
 */
@PacketHandler(opcode = 126)
public class SendPrivateMessageListener implements PacketListener {

    private static final int MIN_PAYLOAD_BYTES = 8;
    private static final int MAX_MESSAGE_BYTES = 256;

    static {
        PacketListenerManager.register(126, new SendPrivateMessageListener());
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < MIN_PAYLOAD_BYTES) {
            return;
        }

        long friend = ByteBufReader.readLong(buf);
        int remaining = buf.readableBytes();
        if (remaining <= 0 || remaining > MAX_MESSAGE_BYTES) {
            return;
        }
        byte[] text = new byte[remaining];
        buf.readBytes(text);
        GameEventBus.post(new PrivateMessageEvent(
                client,
                Utils.longToPlayerName(friend),
                Utils.textUnpack(text, remaining)
        ));
        client.sendPmMessage(friend, text, remaining);
        // no further action; Client handles chat state internally
    }
}
