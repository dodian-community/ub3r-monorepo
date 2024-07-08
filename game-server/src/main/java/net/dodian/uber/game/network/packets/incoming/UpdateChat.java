package net.dodian.uber.game.network.packets.incoming;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.network.packets.Packet;
import net.dodian.utilities.Utils;

public class UpdateChat implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        client.getInputStream().readUnsignedByte();
        client.Privatechat = client.getInputStream().readUnsignedByte();
        client.getInputStream().readUnsignedByte();
        if (System.currentTimeMillis() - client.lastButton < 600) {
            return;
        }
        client.lastButton = System.currentTimeMillis();
        for (int p = 0; p < PlayerHandler.players.length; p++) {
            Client o = client.getClient(p);
            if (client.validClient(p) && o.hasFriend(Utils.playerNameToInt64(client.getPlayerName()))) {
                o.refreshFriends();
            }
        }
    }

}
