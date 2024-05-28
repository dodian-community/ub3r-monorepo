package net.dodian.uber.game.network.packets.incoming;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.network.packets.Packet;

public class MoveItems implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int somejunk = client.getInputStream().readUnsignedWordBigEndianA(); // junk
        @SuppressWarnings("unused")
        int emptySlot = client.getInputStream().readUnsignedByte(); // 255?
        int itemFrom = client.getInputStream().readUnsignedWordBigEndianA(); // slot1
        int itemTo = client.getInputStream().readUnsignedWordBigEndian(); // slot2
        //System.out.println("testing.." + itemFrom + ", " + itemTo);
        client.moveItems(itemFrom, itemTo, somejunk);
    }

}
