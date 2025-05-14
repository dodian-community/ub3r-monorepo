package net.dodian.uber.comm;

import net.dodian.uber.comm.PacketData;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PacketQueue {
    private final Queue<PacketData> packets = new ConcurrentLinkedQueue<>();

    public void add(PacketData p) {
        packets.offer(p);
    }

    public Queue<PacketData> getPackets() {
        return packets;
    }

    public PacketData poll() {
        return packets.poll();
    }

    public boolean isEmpty() {
        return packets.isEmpty();
    }
}