package net.dodian.uber.game.network.packets.outgoing;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.network.packets.OutgoingPacket;

public class InventoryInterface implements OutgoingPacket {

    private int interfaceId, inventoryId;

    public InventoryInterface(int interfaceId, int inventoryId) {
        this.interfaceId = interfaceId;
        this.inventoryId = inventoryId;
    }

    @Override
    public void send(Client client) {
        client.getOutputStream().createFrame(248);
        client.getOutputStream().writeWordA(interfaceId);
        client.getOutputStream().writeWord(inventoryId);
        //client.flushOutStream();
    }

}
