package net.dodian.uber.comm;

import net.dodian.uber.game.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class SocketPoolEntry {
    private Socket socket;

    private long connectedTime;

    private int handedOutCount;

    private long handedOutSince;

    private boolean handedOut;

    private DataOutputStream out = null;

    private DataInputStream in = null;

    public int hashCode() {
        return socket.hashCode();
    }

    public synchronized DataOutputStream getOut() throws IOException {
        if (out == null) {
            out = new DataOutputStream(socket.getOutputStream());
        }
        return out;
    }

    public synchronized DataInputStream getIn() throws IOException {
        if (in == null) {
            in = new DataInputStream(socket.getInputStream());
        }
        return in;
    }

    public SocketPoolEntry(InetAddress address, int port) throws IOException {
        socket = new Socket(address, port);
        DataOutputStream out = getOut();
        try {
            out.writeInt(5);
            out.writeInt(Server.world);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
        connectedTime = System.currentTimeMillis();
        handedOutCount = 0;
        handedOutSince = -1;
        handedOut = false;
        // writeCallbackInfo( socket );
    }

    public long getConnectedTime() {
        return connectedTime;
    }

    public void setConnectedTime(long connectedTime) {
        this.connectedTime = connectedTime;
    }

    public boolean isHandedOut() {
        return handedOut;
    }

    public void setHandedOut(boolean handedOut) {
        this.handedOut = handedOut;
        if (handedOut) {
            handedOutCount++;
            handedOutSince = System.currentTimeMillis();
        }
    }

    public int getHandedOutCount() {
        return handedOutCount;
    }

    public long getHandedOutSince() {
        return handedOutSince;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

}