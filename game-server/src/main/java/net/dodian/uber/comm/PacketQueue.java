package net.dodian.uber.comm;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class PacketQueue {

    /**
     * The list of packets in the queue
     */
    private final Queue<PacketData> packets = new LinkedBlockingQueue<>();

    /**
     * Adds a packet to the queue
     */
    public void add(PacketData p) {
        packets.add(p);
    }

    /**
     * Returns the packets currently in the list and removes them from the backing
     * store
     */
    public LinkedList<PacketData> getPackets() {
        LinkedList<PacketData> tmpList;
        synchronized (packets) {
            tmpList = new LinkedList<>(packets);
            packets.clear();
        }
        return tmpList;
    }

}
