package net.dodian.uber.comm;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class PacketQueue {

    /**
     * The list of packets in the queue
     */
    private Queue<PacketData> packets = new LinkedBlockingQueue<PacketData>();

    /**
     * Adds a packet to the queue
     */
    public void add(PacketData p) {
        packets.add(p);
    }

    /**
     * Returns if there is packets to process
     */
    public boolean hasPackets() {
        return !packets.isEmpty();
    }

    /**
     * Returns the packets currently in the list and removes them from the backing
     * store
     */
    public LinkedList<PacketData> getPackets() {
        LinkedList<PacketData> tmpList = null;
        synchronized (packets) {
            tmpList = new LinkedList<PacketData>();
            tmpList.addAll(packets);
            packets.clear();
        }
        return tmpList;
    }

}
