package net.dodian.uber.game.network.packets.incoming;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.network.packets.Packet;

public class ItemOnGroundItem implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int unknown1 = client.getInputStream().readSignedWordBigEndian(); // interface
        // id
        // of item
        int unknown2 = client.getInputStream().readUnsignedWordA(); // item in bag
        // id
        int floorID = client.getInputStream().readUnsignedByte();
        int floorY = client.getInputStream().readUnsignedWordA();
        int unknown3 = client.getInputStream().readUnsignedWordBigEndianA();
        int floorX = client.getInputStream().readUnsignedByte();
        /*System.out.println("Unknown1 = " + unknown1);
        System.out.println("Unknown2 = " + unknown2);
        System.out.println("FloorID = " + floorID);
        System.out.println("FloorY = " + floorY);
        System.out.println("Unknown3 = " + unknown3);
        System.out.println("FloorX = " + floorX);*/
    }

}
