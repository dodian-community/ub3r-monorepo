package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.systems.world.player.PlayerRegistry;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.systems.interaction.PlayerTickThrottleService;
import net.dodian.uber.game.systems.net.PacketConnectionService;
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
        ByteBuf buf = packet.payload();
        // Legacy packet structure: [byte toggle?][byte privateChat][byte unknown]
        buf.readUnsignedByte();
        int priv = buf.readUnsignedByte();
        buf.readUnsignedByte();

        if (!PlayerTickThrottleService.tryAcquireMs(client, PlayerTickThrottleService.CHAT_PRIVACY, 600L)) {
            return; // anti-spam
        }
        PacketConnectionService.setPrivateChatMode(client, priv);

        // Notify friends so their list icon updates
        for (int i = 0; i < PlayerRegistry.players.length; i++) {
            Client other = client.getClient(i);
            if (client.validClient(i) && other.hasFriend(Utils.playerNameToInt64(client.getPlayerName()))) {
                other.refreshFriends();
            }
        }
        logger.debug("UpdateChatListener: {} set private chat={} and refreshed friends", client.getPlayerName(), priv);
    }
}

