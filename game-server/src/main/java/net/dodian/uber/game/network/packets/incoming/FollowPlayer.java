package net.dodian.uber.game.network.packets.incoming;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.network.packets.Packet;

public class FollowPlayer implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        @SuppressWarnings("unused")
        int followId = client.getInputStream().readSignedWordBigEndian();
        Client player = client.getClient(followId);
        if(player != null) {
            String url = "https://dodian.net/index.php?pageid=modcp&action=search&player=";
            url += player.getPlayerName().replaceAll(" ", "%20");
            Player.openPage(client, url);
        }
    }

}
