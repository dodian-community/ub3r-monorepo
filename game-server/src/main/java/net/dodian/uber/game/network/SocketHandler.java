package net.dodian.uber.game.network;

import net.dodian.uber.comm.PacketData;
import net.dodian.uber.game.Constants;
import net.dodian.uber.game.model.YellSystem;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.utilities.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(SocketHandler.class);

    private final Client player;
    private final SocketChannel socketChannel;
    private final Queue<PacketData> myPackets = new ConcurrentLinkedQueue<>();
    private final Queue<byte[]> outData = new ConcurrentLinkedQueue<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public SocketHandler(Client player, SocketChannel socketChannel) {
        this.player = player;
        this.socketChannel = socketChannel;
        logger.info("SocketHandler initialized for player: {}", player != null ? player.getPlayerName() : "null");
    }

    public void processPacket(PacketData packet) {
        logger.info("Processing packet for player: {} - Packet ID: {}", player != null ? player.getPlayerName() : "null", packet.getId());
        myPackets.add(packet);
        sendPackets();
    }

    public void queueOutput(byte[] array) {
        if (isConnected()) {
            outData.add(array);
            writeOutput();
        }
    }

    public void writeByte(byte b) {
        if (isConnected()) {
            ByteBuffer buffer = ByteBuffer.wrap(new byte[]{b});
            lock.writeLock().lock();
            try {
                socketChannel.write(buffer);
            } catch (IOException e) {
                handleDisconnect(e, "writeByte");
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    private void handleDisconnect(IOException e, String action) {
        if (player != null) {
            player.disconnected = true;
            YellSystem.alertStaff(player.getPlayerName() + " has disconnected unexpectedly!");
        }
        cleanup();
        logger.error("SocketHandler: Failed to {}: {}", action, e.getMessage(), e);
    }

    public void logout() {
        if (player != null) {
            player.disconnected = true;
            YellSystem.alertStaff(player.getPlayerName() + " has logged out correctly!");
        }
        cleanup();
    }

    private void cleanup() {
        try {
            if (socketChannel != null && socketChannel.isOpen()) {
                socketChannel.close();
            }
        } catch (IOException e) {
            logger.error("SocketHandler Cleanup Exception: {}", e.getMessage(), e);
        }
    }

    public void parsePackets(ByteBuffer buffer) {
        while (buffer.remaining() > 0) {
            parsePacket(buffer);
        }
    }

    private void parsePacket(ByteBuffer buffer) {
        if (player.packetType == -1 && buffer.remaining() > 0) {
            player.packetType = buffer.get() & 0xff;
            if (player.inStreamDecryption != null) {
                player.packetType = player.packetType - player.inStreamDecryption.getNextKey() & 0xff;
            }
            player.packetSize = Constants.PACKET_SIZES[player.packetType];
        }

        if (player.packetSize == -1 && buffer.remaining() > 0) {
            player.packetSize = buffer.get() & 0xff;
        }

        if (buffer.remaining() < player.packetSize) {
            return;
        }

        byte[] data = new byte[player.packetSize];
        buffer.get(data, 0, player.packetSize);

        Stream inStream = new Stream(data);
        if (player.inStreamDecryption != null) {
            player.inStreamDecryption.init(inStream);
        }

        PacketData pData = new PacketData(player.packetType, data, player.packetSize);
        myPackets.add(pData);

        player.timeOutCounter = 0;
        player.packetType = -1;
    }

    private void sendPackets() {
        if (!myPackets.isEmpty() && isConnected()) {
            lock.writeLock().lock();
            try {
                while (!myPackets.isEmpty()) {
                    PacketData packet = myPackets.poll();
                    if (packet != null && socketChannel != null) {
                        ByteBuffer buffer = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
                        socketChannel.write(buffer);
                    }
                }
            } catch (IOException e) {
                handleDisconnect(e, "sendPackets");
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    private void writeOutput() {
        if (!outData.isEmpty() && isConnected()) {
            lock.writeLock().lock();
            try {
                while (!outData.isEmpty() && isConnected()) {
                    byte[] data = outData.poll();
                    if (data != null && socketChannel != null) {
                        ByteBuffer buffer = ByteBuffer.wrap(data);
                        try {
                            socketChannel.write(buffer);
                        } catch (IOException e) {
                            if ("Broken pipe".equals(e.getMessage())) {
                                logger.error("Broken pipe error while writing to client: {}", player != null ? player.getPlayerName() : "null");
                                handleDisconnect(e, "writeOutput");
                                return;
                            }
                        }
                    }
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    private boolean isConnected() {
        return socketChannel != null && socketChannel.isConnected() && player != null && !player.disconnected;
    }

    public Queue<PacketData> getPackets() {
        return myPackets;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }
}