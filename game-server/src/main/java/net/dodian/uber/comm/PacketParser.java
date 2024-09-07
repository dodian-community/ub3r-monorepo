package net.dodian.uber.comm;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.model.entity.player.Client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Queue;

public class PacketParser {

    private static final int VARIABLE_BYTE = -1;
    private static final int VARIABLE_SHORT = -2;

    private static final ThreadLocal<ByteBuffer> bufferPool = ThreadLocal.withInitial(
            () -> ByteBuffer.allocateDirect(SocketHandler.BUFFER_SIZE)
    );

    private int packetSize = 0;
    private int packetType = -1;

    public void parsePackets(ByteBuffer inputBuffer, Client player, Queue<PacketData> incomingPackets) throws IOException {
        ByteBuffer packetBuffer = bufferPool.get();
        packetBuffer.clear();  // Reset the buffer for reuse

        while (inputBuffer.hasRemaining()) {
            if (packetType == -1) {
                packetType = inputBuffer.get() & 0xff;
                packetType = player.inStreamDecryption != null
                        ? (packetType - player.inStreamDecryption.getNextKey() & 0xff)
                        : packetType;
                packetSize = getPacketSize(packetType);
            }

            if (packetSize == VARIABLE_BYTE) {
                if (inputBuffer.remaining() < 1) return;
                packetSize = inputBuffer.get() & 0xff;
            } else if (packetSize == VARIABLE_SHORT) {
                if (inputBuffer.remaining() < 2) return;
                packetSize = inputBuffer.getShort() & 0xffff;
            }

            if (inputBuffer.remaining() < packetSize) return;

            // Ensure packetBuffer has enough capacity
            if (packetBuffer.capacity() < packetSize) {
                packetBuffer = ByteBuffer.allocateDirect(packetSize);
                bufferPool.set(packetBuffer);
            }

            // Read data into packetBuffer
            packetBuffer.clear();
            for (int i = 0; i < packetSize; i++) {
                packetBuffer.put(inputBuffer.get());
            }
            packetBuffer.flip();

            // Create byte array from packetBuffer
            byte[] data = new byte[packetSize];
            packetBuffer.get(data);

            incomingPackets.offer(new PacketData(packetType, data, packetSize));

            packetType = -1;
            packetSize = 0;
        }
    }

    private int getPacketSize(int packetType) {
        if (packetType < 0 || packetType >= Constants.PACKET_SIZES.length) {
            return -1; // Invalid packet type
        }
        return Constants.PACKET_SIZES[packetType];
    }
}