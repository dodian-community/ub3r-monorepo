package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.utilities.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty port of UpdateChat (opcode 95) – adjusts private chat mode and refreshes friends lists.
 */
public class UpdateChatListener implements PacketListener {

    static { PacketListenerManager.register(95, new UpdateChatListener()); }

    private static final Logger logger = LoggerFactory.getLogger(UpdateChatListener.class);

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();
        // Legacy packet structure: [byte toggle?][byte privateChat][byte unknown]
        buf.readUnsignedByte();
        int priv = buf.readUnsignedByte();
        buf.readUnsignedByte();

        if (System.currentTimeMillis() - client.lastButton < 600) {
            return; // anti-spam
        }
        client.lastButton = System.currentTimeMillis();
        client.Privatechat = priv;

        // Notify friends so their list icon updates
        for (int i = 0; i < PlayerHandler.players.length; i++) {
            Client other = client.getClient(i);
            if (client.validClient(i) && other.hasFriend(Utils.playerNameToInt64(client.getPlayerName()))) {
                other.refreshFriends();
            }
        }
        logger.debug("UpdateChatListener: {} set private chat={} and refreshed friends", client.getPlayerName(), priv);
    }
}
