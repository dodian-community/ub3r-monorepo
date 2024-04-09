package net.dodian.uber.comm;

import java.util.Arrays;

public class PacketData {

    private final int id;
    private int length;
    private final byte[] data;

    public PacketData(int id, byte[] data, int length) {
        this.id = id;
        this.data = data;
        this.length = length;
    }

    public int getId() {
        return id;
    }

    public int getLength() {
        return length;
    }

    public byte[] getData() {
        return data;
    }

    public void setLength(int l) {
        this.length = l;
    }

    public String toString() {
        return "[" + id + "]" + Arrays.toString(data);
    }

}
