package net.dodian.uber.game.model.player.packets.incoming;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.networking.game.ByteBufPacket;


public class ChangeRegionBuf implements ByteBufPacket {

    @Override
    public void process(Client client, int opcode, int size, ByteBuf payload) {
        // Mark player as fully loaded once for new sessions
        if (!client.pLoaded) {
            client.pLoaded = true;
        }

        int wild = client.getWildLevel();
        if (wild > 0) {
            client.setWildLevel(wild);
        } else {
            client.updatePlayerDisplay();
        }

        // Ensure private-message list arrives once
        if (!client.IsPMLoaded) {
            client.refreshFriends();
            client.IsPMLoaded = true;
        }

        // Legacy handler called customObjects() only for opcode 121 (load area)
        if (opcode == 121) {
            client.customObjects();
        }
    }
}
