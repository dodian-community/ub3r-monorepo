package net.dodian.uber.comm;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.model.YellSystem;
import net.dodian.uber.game.model.entity.player.Client;

import java.nio.ByteBuffer;
import java.util.Queue;

public class PacketParser {
    private static final int VARIABLE_BYTE = -1;
    private static final int VARIABLE_SHORT = -2;
    private static final int MAX_PACKETS_PER_SECOND = 20;
    private static final long WINDOW_SIZE_MS = 600;

    private int packetSize = 0;
    private int packetType = -1;

    private long lastWindowStart = System.currentTimeMillis();
    private int packetsInWindow = 0;
    private int attemptedPackets = 0;
    private long lastFloodAlert = 0;

    public void parsePackets(ByteBuffer inputBuffer, Client player, Queue<PacketData> incomingPackets) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastWindowStart >= WINDOW_SIZE_MS) {
            lastWindowStart = currentTime;
            packetsInWindow = 0;
            attemptedPackets = 0;
        }

        while (inputBuffer.hasRemaining()) {
            attemptedPackets++;

            if (attemptedPackets > MAX_PACKETS_PER_SECOND &&
                    (currentTime - lastFloodAlert > WINDOW_SIZE_MS)) {
                if (player != null) {
                    System.out.println("Warning: Packet flood attempted from player: "
                            + player.getPlayerName() + " - Attempted: " + attemptedPackets
                            + " packets in " + WINDOW_SIZE_MS + "ms");
                    YellSystem.alertStaff("Warning: Packet flood attempted from player: "
                            + player.getPlayerName() + " - Attempted: " + attemptedPackets
                            + " packets in " + WINDOW_SIZE_MS + "ms");
                }
                lastFloodAlert = currentTime;
            }

            if (packetsInWindow >= MAX_PACKETS_PER_SECOND) {
                continue;
            }

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
            packetsInWindow++;

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
