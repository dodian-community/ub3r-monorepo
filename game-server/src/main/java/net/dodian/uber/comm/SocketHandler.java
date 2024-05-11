package net.dodian.uber.comm;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.model.YellSystem;
import net.dodian.uber.game.model.entity.player.Client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class SocketHandler implements Runnable {

    private final Client player;
    private final Socket socket;
    private final AtomicBoolean processRunning = new AtomicBoolean(true);

    private final Queue<PacketData> myPackets = new ConcurrentLinkedQueue<>();
    private final Queue<byte[]> outData = new ConcurrentLinkedQueue<>();

    private InputStream inputStream;
    private OutputStream outputStream;

    public SocketHandler(Client player, Socket socket) {
        this.player = player;
        this.socket = socket;
        initializeStreams();
    }

    private void initializeStreams() {
        try {
            this.inputStream = this.socket.getInputStream();
            this.outputStream = this.socket.getOutputStream();
        } catch (IOException e) {
            YellSystem.alertStaff("SocketHandler: Failed to initialize streams: " + e.getMessage());
            player.disconnected = true;
        }
    }

    public InputStream getInput() {
        return inputStream;
    }

    public OutputStream getOutput() {
        return outputStream;
    }

    @Override
    public void run() {
        while (processRunning.get() && isConnected() && !player.disconnected) {
            writeOutput();
            flush();
            parsePackets();
            player.timeOutCounter = 0;
        }
        cleanup();
    }

    private boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected() && !player.disconnected;
    }

    public void flush() {
        try {
            if (outputStream != null) {
                synchronized (outputStream) {
                    outputStream.flush();
                }
            }
        } catch (IOException e) {
            player.disconnected = true;
            System.out.println("SocketHandler: Failed to flush output stream: " + e.getMessage());
        }
    }

    public void write(byte[] array) {
        try {
            if (outputStream != null) {
                synchronized (outputStream) {
                    outputStream.write(array);
                }
            }
        } catch (IOException e) {
            player.disconnected = true;
            System.out.println("SocketHandler: Failed to write output array: " + e.getMessage());
        }
    }

    public void write(byte[] data, int off, int length) {
        try {
            if (outputStream != null) {
                synchronized (outputStream) {
                    outputStream.write(data, off, length);
                }
            }
        } catch (IOException e) {
            player.disconnected = true;
            System.out.println("SocketHandler: Failed to write output data: " + e.getMessage());
        }
    }

    public void logout() {
        if (processRunning.getAndSet(false)) {
            player.disconnected = true;
            cleanup();
            YellSystem.alertStaff(player.getPlayerName() + " has logged out correctly!");
        }
    }

    private void cleanup() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("SocketHandler Cleanup Exception: " + e.getMessage());
        }
    }

    private void parsePackets() {
        try {
            if (player.disconnected || inputStream == null) {
                processRunning.set(false);
                return;
            }

            int avail = inputStream.available();
            if (avail == 0) {
                return;
            }

            if (player.packetType == -1) {
                player.packetType = inputStream.read() & 0xff;
                if (player.inStreamDecryption != null) {
                    player.packetType = player.packetType - player.inStreamDecryption.getNextKey() & 0xff;
                }
                player.packetSize = Constants.PACKET_SIZES[player.packetType];
                avail--;
            }

            if (player.packetSize == -1) {
                if (avail > 0) {
                    player.packetSize = inputStream.read() & 0xff;
                    avail--;
                } else {
                    return;
                }
            }

            if (avail < player.packetSize) {
                return;
            }

            fillInStream(player.packetType, player.packetSize);
            player.timeOutCounter = 0;
            player.packetType = -1;
        } catch (IOException e) {
            player.disconnected = true;
            processRunning.set(false);
            System.out.println("SocketHandler: Error in parsePackets: " + e.getMessage());
        }
    }

    private void fillInStream(int id, int forceRead) throws IOException {
        byte[] data = new byte[forceRead];
        inputStream.read(data, 0, forceRead);
        PacketData pData = new PacketData(id, data, forceRead);
        myPackets.add(pData);
    }

    public Queue<PacketData> getPackets() {
        return myPackets;
    }

    public void queueOutput(byte[] copy) {
        outData.add(copy);
    }

    public void writeOutput() {
        for (int i = 0; i < 20; i++) {
            if (outData.isEmpty()) {
                return;
            }
            byte[] data = outData.poll();
            if (data != null) {
                write(data);
            }
        }
    }
}