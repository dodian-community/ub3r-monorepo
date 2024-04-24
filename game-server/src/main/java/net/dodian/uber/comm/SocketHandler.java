package net.dodian.uber.comm;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.model.YellSystem;
import net.dodian.uber.game.model.entity.player.Client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SocketHandler implements Runnable {

    private final Client player;
    private final Socket socket;

    private boolean processRunning;

    private final PacketQueue packets = new PacketQueue();
    private final Queue<PacketData> myPackets = new LinkedList<>();
    private final Queue<byte[]> outData = new ConcurrentLinkedQueue<>();

    public SocketHandler(Client player, Socket socket) {
        this.player = player;
        this.socket = socket;
        this.processRunning = true;
    }

    public void run() {
        try {
        while (processRunning) {
            if (isConnected()) {
                writeOutput();
                parsePackets();
                LinkedList<PacketData> temp = packets.getPackets();
                if (temp != null) myPackets.addAll(temp);
            } else {
                myPackets.clear();
                break;
            }
        }
            Thread.sleep(50);
            } catch (java.lang.Exception _ex) {
                YellSystem.alertStaff("Something is up with socket handler!");
                System.out.println("SocketHandling is throwing errors: " + _ex.getMessage());
            }
    }

    private boolean isConnected() {
        return getInput() != null && getOutput() != null;
    }

    public Client getPlayer() {
        return player;
    }

    public Socket getSocket() {
        return socket;
    }

    public InputStream getInput() {
        try {
            return getSocket().getInputStream();
        } catch (IOException e) {
            logout();
        }
        return null;
    }

    public OutputStream getOutput() {
        try {
            return getSocket().getOutputStream();
        } catch (IOException e) {
            logout();
        }
        return null;
    }

    public void write(byte[] array) {
        try {
            getOutput().write(array);
        } catch (IOException e) {
            logout();
        }
    }

    public void write(byte[] data, int i, int length) {
        try {
            getOutput().write(data, i, length);
        } catch (IOException e) {
            logout();
        }
    }

    public void logout() {
        this.processRunning = false;
        if(player == null || player.disconnected) { //Fuck this check!
            return;
        }
        if(player.UsingAgility)
            player.xLog = true;
        else player.disconnected = true;
    }

    public void parsePackets() {
        try {
            if (player.disconnected) {
                this.processRunning = false;
                return;
            }
            if (getInput() == null) {
                this.processRunning = false;
                return;
            }

            int avail = getInput().available();
            if (avail == 0) {
                return;
            }
            if (player.packetType == -1) {
                player.packetType = getInput().read() & 0xff;
                if (player.inStreamDecryption != null) {
                    player.packetType = player.packetType - player.inStreamDecryption.getNextKey() & 0xff;
                }
                player.packetSize = Constants.PACKET_SIZES[player.packetType];
                avail--;
            }
            if (player.packetSize == -1) {
                if (avail > 0) {
                    player.packetSize = getInput().read() & 0xff;
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
        } catch (java.lang.Exception __ex) {
            //player.saveStats(true); //We do not need this if we disconnect!
            player.disconnected = true;
            this.processRunning = false;
        }
    }

    private void fillInStream(int id, int forceRead) throws IOException {
        byte[] data = new byte[forceRead];
        getInput().read(data, 0, forceRead);
        //write(data, 0, forceRead);
        PacketData pData = new PacketData(id, data, forceRead);
        packets.add(pData);
    }

    public LinkedList<PacketData> getPackets() {
        return (LinkedList<PacketData>) myPackets;
    }

    public void queueOutput(byte[] copy) {
        outData.add(copy);
    }

    public void writeOutput() {
        for (int i = 0; i < 20; i++) {
            if (outData.isEmpty())
                return;
            byte[] data = outData.poll();
            if (data != null)
                write(data);
        }
    }

}
