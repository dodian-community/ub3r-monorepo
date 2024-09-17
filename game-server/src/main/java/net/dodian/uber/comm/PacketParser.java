package net.dodian.uber.comm;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.model.entity.player.Client;

import java.nio.ByteBuffer;
import java.util.Queue;

public class PacketParser {

    private static final int VARIABLE_BYTE = -1;
    private static final int VARIABLE_SHORT = -2;

    private int packetSize = 0;
    private int packetType = -1;

    public void parsePackets(ByteBuffer inputBuffer, Client player, Queue<PacketData> incomingPackets) {
        while (inputBuffer.hasRemaining()) {
            if (packetType == -1) {
                if (!inputBuffer.hasRemaining()) return;
                packetType = inputBuffer.get() & 0xff;
                packetType = player.inStreamDecryption != null
                        ? (packetType - player.inStreamDecryption.getNextKey() & 0xff)
                        : packetType;
                packetSize = getPacketSize(packetType);
            }

            if (packetSize == VARIABLE_BYTE) {
                if (!inputBuffer.hasRemaining()) return;
                packetSize = inputBuffer.get() & 0xff;
            } else if (packetSize == VARIABLE_SHORT) {
                if (inputBuffer.remaining() < 2) return;
                packetSize = inputBuffer.getShort() & 0xffff;
            }

            if (inputBuffer.remaining() < packetSize) return;

            byte[] data = new byte[packetSize];
            inputBuffer.get(data);

            incomingPackets.offer(new PacketData(packetType, data, packetSize));

            packetType = -1;
            packetSize = 0;
        }
    }

    private int getPacketSize(int packetType) {
        return (packetType >= 0 && packetType < Constants.PACKET_SIZES.length)
                ? Constants.PACKET_SIZES[packetType]
                : -1;
    }
}
