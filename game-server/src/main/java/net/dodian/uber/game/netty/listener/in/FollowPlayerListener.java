package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opcode 39 – Follow player (opens mod-cp search page for target player).
 */
public class FollowPlayerListener implements PacketListener {

    static { PacketListenerManager.register(39, new FollowPlayerListener()); }

    private static final Logger logger = LoggerFactory.getLogger(FollowPlayerListener.class);

    private static int readSignedWordBigEndian(ByteBuf buf) {
        int low = buf.readUnsignedByte();
        int high = buf.readUnsignedByte();
        int value = (high << 8) | low;
        if (value > 32767) value -= 65536;
        return value;
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        int followId = readSignedWordBigEndian(packet.getPayload());
        if (client.getSlot() == followId) return; // cannot follow yourself

        Client player = client.getClient(followId);
        if (player == null) return;

        if (logger.isTraceEnabled()) {
            logger.trace("FollowPlayer from={} targetSlot={} targetName={}", client.getPlayerName(), followId, player.getPlayerName());
        }

        client.resetWalkingQueue();
        String url = "https://dodian.net/index.php?pageid=modcp&action=search&player=" + player.getPlayerName().replace(" ", "%20");
        Player.openPage(client, url);
    }
}
